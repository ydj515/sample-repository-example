# Redis 업무 시나리오

기존 명령어 Router와 `/strategy`를 유지하고 `/usecases` 아래에 업무 예제를 제공한다.
Java 21, 프로젝트 Gradle Wrapper, localhost:6379의 Redis를 사용한다.

```bash
SPRING_DATA_REDIS_PASSWORD= mise exec -- ./gradlew bootRun
```

패키지는 각 계층의 `usecase/{product,cart,popularity,inventory,orderevent}`로 구성한다.
Router는 경로, Handler는 HTTP 입출력, Service는 업무 흐름, Repository는 Redis 연산을 담당한다.
모든 샘플 키는 `usecase:`로 시작한다. 실제 데이터와 분리한 로컬 Redis에서 실행한다.

## 1. 상품 캐시

상품 `1`(Keyboard), `2`(Mouse)를 제공하는 메모리 Fake DB에 200ms 지연을 둔다.
캐시 TTL은 30초이며, 조회는 cache-aside로 수행한다.
`protected=true`는 Redisson 락 획득 후 캐시를 다시 확인해 중복 DB 로딩을 줄인다.

```bash
curl 'http://localhost:8080/usecases/products/1?protected=true'
curl 'http://localhost:8080/usecases/products/1/ttl'
curl 'http://localhost:8080/usecases/products/stats/db'
```

`protected=false`와 비교하려면 캐시 만료 후 동시에 여러 요청을 보내고 `dbReads` 증가량을 확인한다.
`dbReads`는 애플리케이션 인스턴스의 누적 조회 횟수다. 존재하지 않는 상품은 404다.
락 대기 시간은 2초, lease는 5초다. 대기 실패는 503이며 lease보다 로딩이 길어지면 중복 로딩이 가능하다.
Redis 오류는 전파한다. DB fallback이나 운영 성능 향상을 보장하는 예제가 아니다.

## 2. 장바구니와 세션

세션과 장바구니는 Hash로 저장하고 접근할 때 30분 TTL을 갱신한다.
Lua로 Hash 읽기/쓰기와 해당 키의 TTL 갱신을 묶는다.
세션 ID는 학습용이며 사용자 인증을 구현하지 않는다.

```bash
curl -X POST localhost:8080/usecases/sessions \
  -H 'Content-Type: application/json' -d '{"userId":"user-1"}'
# 응답의 sessionId를 넣는다.
curl -X PUT localhost:8080/usecases/carts/SESSION_ID/items \
  -H 'Content-Type: application/json' -d '{"productId":"1","quantity":2}'
curl localhost:8080/usecases/carts/SESSION_ID
```

수량은 누적 추가가 아닌 설정값이다. 0이면 해당 항목을 제거한다.
만료된 세션은 401, 음수 수량은 400이다. 세션 갱신과 장바구니 변경은 서로 다른 Lua 호출이므로
두 키를 아우르는 하나의 트랜잭션은 아니다. 상품 존재 여부와 가격 검증은 구현하지 않는다.

## 3. 찜, 랭킹, 방문

```bash
curl -X PUT localhost:8080/usecases/popularity/wishlists/user-1/1
curl localhost:8080/usecases/popularity/wishlists/user-1
curl -X POST localhost:8080/usecases/popularity/ranking/1
curl 'localhost:8080/usecases/popularity/ranking?limit=10'
curl -X POST localhost:8080/usecases/popularity/visits/1/42
curl localhost:8080/usecases/popularity/visits/1/42
```

- Set: 사용자별 찜 중복 방지. `added=0`은 이미 존재하는 찜이다.
- ZSet: 명시적인 점수 증가와 상위 순위 조회. 찜 추가와 점수 증가는 독립 예제다.
- HyperLogLog: 상품별 일일 고유 방문자 추정치. 정확한 수가 아니다.
- Bitmap: 같은 날 특정 방문자의 방문 여부. 방문자는 0..1,000,000 정수로 제한한다.

방문 날짜는 UTC이며 방문 키는 접근 시 7일 TTL을 갱신한다. 찜과 랭킹은 자동 만료하지 않는다.
방문 기록의 HLL/Bitmap/TTL 명령들은 하나의 원자 연산이 아니므로 중간 실패 시 부분 반영될 수 있다.
랭킹 limit은 1..100이다.

## 4. 재고 동시성 재현

```bash
curl -X POST localhost:8080/usecases/inventory/simulations \
  -H 'Content-Type: application/json' \
  -d '{"mode":"lua","initialStock":10,"requests":50,"concurrency":20,"quantity":1}'
```

| mode | 방식 | 관찰 항목 |
|---|---|---|
| unsafe | GET → 20ms 지연 → SET | 읽기 경쟁으로 성공 건수와 최종 재고 불일치 가능 |
| atomic | DECRBY | 감소는 원자적이나 부족 요청도 감소시켜 음수 재고 발생 |
| lock | 락 → 확인 → 감소 | 모든 작성자가 같은 락을 따르는 동안 부족 요청 거절 |
| lua | 확인과 DECRBY를 단일 스크립트로 실행 | 부족 요청을 감소 없이 거절 |

응답의 `expectedRemaining = initialStock - accepted × quantity`와 실제 `remaining`을 비교한다.
`consistent`는 두 값이 같고 실제 재고가 음수가 아닌지를 나타낸다.
각 실행은 UUID 키를 생성하고 10분 후 만료한다. 요청 수는 1..1000, 동시성은 1..100,
초기 재고는 0..1,000,000, 요청 수량은 1..1,000,000으로 제한한다.
락 대기 5초/lease 10초를 초과하면 실패할 수 있다. 주문 ID에 의한 멱등성은 구현하지 않는다.

## 5. 주문 이벤트 파이프라인

`list`, `pubsub`, `stream`별로 발행한다. 외부 시스템에 전송하지 않고 후처리 결과를 응답으로 반환한다.
자동 백그라운드 워커 대신 소비 API로 처리 시점을 관찰한다.

```bash
curl -X POST localhost:8080/usecases/order-events/list/publish \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"order-1","productId":"1","quantity":2}'
curl -X POST localhost:8080/usecases/order-events/list/process-next

# 별도 터미널에서 구독한 뒤 pubsub/publish를 호출한다.
curl -N localhost:8080/usecases/order-events/pubsub/subscribe

# stream/publish로 이벤트를 발행한 뒤 그룹 생성 및 처리한다.
curl -X POST localhost:8080/usecases/order-events/stream/group
curl -X POST 'localhost:8080/usecases/order-events/stream/process?consumer=worker-1'
curl -X POST 'localhost:8080/usecases/order-events/stream/process?consumer=worker-1&pending=true'
```

- List: RPUSH/LPOP의 FIFO. 빈 큐는 204. pop 후 처리 실패 시 이벤트가 유실될 수 있다.
- Pub/Sub: 접속한 구독자에게 SSE 전달. 구독자가 없으면 발행 결과는 0이며 메시지를 보관하지 않는다.
- Stream: `order-workers` 그룹에서 최대 20개를 순차 처리하고 성공 후 ACK한다.
  실패한 이벤트는 pending에 남고 같은 consumer의 `pending=true`로 다시 처리한다.
  다른 consumer의 pending은 아래 복구 API로 회수한다.

Stream은 처리 후 ACK 직전 실패하면 중복 처리가 가능하다. 실제 부수 효과에는 orderId 멱등 처리가 필요하다.
ACK는 스트림 레코드를 삭제하지 않는다. 보관량 제한과 완료 보장은 제공하지 않는다.

### 중단된 소비자 복구

[실행 요청 파일](../http/usecase-stream-recovery.http)을 번호 순서대로 실행한다.
`reserve`는 메시지를 읽고 ACK하지 않아 소비자 중단 후의 pending 상태를 재현한다.

```bash
curl -X POST 'localhost:8080/usecases/order-events/stream/reserve?consumer=stopped-worker'
curl 'localhost:8080/usecases/order-events/stream/pending?cursor=0-0&count=20'
# reserve 이후 최소 1초가 지난 뒤 실행
curl -X POST 'localhost:8080/usecases/order-events/stream/recover?consumer=recovery-worker&minIdleMillis=1000&cursor=0-0&count=20'
```

Stream 키는 `app.usecase.order-stream.key`로 지정할 수 있으며 기본값은 `usecase:orders:stream`이다.
통합 테스트는 실행마다 UUID Stream 키를 사용한다.
현재 Spring Data Redis 3.4.4의 Lettuce pending 범위 변환 오류를 피하기 위해
XPENDING은 `template.execute()`로 Lua를 실행하고, 회수는 `template.opsForStream().claim()`을 사용한다.

복구는 XPENDING 페이지 조회 → idle 조건으로 후보 선택 → XCLAIM → 순차 처리 → 성공 후 ACK 순서다.
Redis가 XCLAIM 시점에 idle 시간을 다시 확인하므로 후보 조회 후 다시 전달된 메시지는 회수되지 않을 수 있다.
응답은 `consumer`, `scanned`, `processed`, `nextCursor`를 제공한다.
`processed`의 `acknowledged=1`은 해당 레코드 ACK 성공이다.
`nextCursor`가 `0-0`이 아니면 그 값을 다음 요청의 cursor로 전달한다. 다음 범위는 cursor를 제외하고 조회한다.
이번 페이지에서 회수한 메시지가 없어도 커서가 남으면 다음 페이지를 조회한다.
`0-0`은 이번 스캔 종료이며 idle 미달 메시지가 없다는 뜻은 아니다. 시간이 지난 뒤 다시 `0-0`에서 시작한다.

`count`는 1..100, `minIdleMillis`는 1..86,400,000이며 기본 idle 기준은 60초다.
consumer는 영문·숫자·밑줄·하이픈 1..64자로 제한한다.
이전 소비자의 소유권을 회수할 뿐 이전 워커의 실행을 중단시키지는 않는다.
정상 처리 시간보다 낮은 idle 기준은 실행 중인 메시지를 회수해 중복 부수 효과를 만들 수 있다.
처리 실패 시 ACK하지 않고 회수 소비자의 pending에 남긴다. 같은 소비자의 `pending=true`로 재시도할 수 있다.
재시도 제한과 DLQ는 아래 예제에서 제공한다. 자동 복구 스케줄러는 포함하지 않는다.

페이지 크기를 C, 전체 pending 수를 N이라고 하면 조회와 회수의 서버 비용은 O(C log N),
응답 데이터 공간은 O(C)다. 실제 후처리 비용과 메시지 크기는 별도다.
Redis 명령 기준은 [XPENDING](https://redis.io/docs/latest/commands/xpending/)과
[XCLAIM](https://redis.io/docs/latest/commands/xclaim/) 문서를 참고한다.

### 재시도 제한과 DLQ

[실행 요청 파일](../http/usecase-stream-dlq.http)을 순서대로 실행한다.
Stream `process`와 `recover`에 `fail=true`를 지정하면 샘플 처리 실패를 재현한다.
기본값은 false이며 List와 Pub/Sub 소비에는 적용하지 않는다.
처리 실패는 HTTP 200 결과의 상태로 반환하여 같은 배치의 다른 메시지도 계속 처리한다.

| 상태 | 의미 |
|---|---|
| PROCESSED | 처리 성공 후 ACK |
| RETRY_PENDING | 한도 미달 처리 실패. ACK 없이 pending 유지 |
| DEAD_LETTERED | 한도 도달. DLQ 기록 후 원본 ACK |
| NOT_PENDING | 이미 ACK된 레코드. 추가 DLQ 기록 없음 |
| OWNERSHIP_CHANGED | 다른 소비자가 소유권을 회수함. 실패 기록/ACK 없음 |

기본 한도는 첫 처리 포함 3회 실패, 즉 최초 실패 후 최대 두 번 재시도다.
설정 `app.usecase.order-stream.max-attempts`는 1..10이며, Redis Hash로 레코드별 실패 횟수를 공유한다.
XREADGROUP/XCLAIM 전달 횟수나 reserve 호출은 처리 실패 횟수로 세지 않는다.
처리 성공 시 실패 메타데이터를 정리한다. ACK/Redis 오류도 처리 실패 횟수에 포함하지 않는다.
처리기 오류는 클래스 이름만 저장하고 예외 메시지·스택은 저장하지 않는다.

재시도는 같은 소비자의 `process?pending=true` 또는 idle 이후 `recover` 요청으로 수행한다.
요청 내 자동 반복이나 backoff 스케줄러는 없다. 배치마다 최대 20개를 읽는다.
한도에 도달하면 Lua가 pending 소유권 확인 → DLQ XADD → 원본 XACK → 실패 메타데이터 정리를 수행한다.
DLQ 키는 원본 Stream 키에 `:dlq`를 붙인다. `event` 원문, `originalRecordId`, `sourceStream`,
`group`, `attempts`, `errorType`을 보관한다.

```bash
curl -X POST 'localhost:8080/usecases/order-events/stream/process?consumer=dlq-worker&fail=true'
curl -X POST 'localhost:8080/usecases/order-events/stream/process?consumer=dlq-worker&pending=true&fail=true'
curl -X POST 'localhost:8080/usecases/order-events/stream/process?consumer=dlq-worker&pending=true&fail=true'
curl 'localhost:8080/usecases/order-events/stream/dlq?count=100'
```

DLQ 조회는 오래된 레코드부터 최대 count개를 반환하며 count는 1..100이다.
DLQ는 자동 만료/삭제/재발행하지 않는다. 원본 Stream 레코드도 ACK만 하고 삭제하지 않는다.
Lua 실행은 명령 사이의 경쟁을 막지만 오류 시 이전 쓰기를 롤백하지 않는다.
DLQ XADD 실패 시 ACK를 수행하지 않으므로 원본은 pending에 남고 실패 메타데이터도 유지한다.
이후 요청에서는 처리기를 다시 실행하지 않고 DLQ 이동만 재시도한다.
실제 워커 중단으로 실패 횟수를 기록하지 못한 실행과 동시 중복 실행까지 제한하지는 않는다.
소유권 회수는 이전 워커의 부수 효과를 취소하지 않으므로 실제 처리에는 별도 멱등성이 필요하다.

- 제약: 현재 설정은 단일 Redis다. Cluster에 적용하려면 스크립트 키들을 같은 hash slot에 배치해야 한다.
- 위험: DLQ와 원본 Stream의 무제한 보관은 저장량 증가를 유발한다.
- 예외: Redis/DLQ 쓰기 오류는 HTTP 오류로 전파한다. DLQ 상태 반환은 성공적으로 이동한 경우에만 이루어진다.

한 레코드의 실패 기록과 DLQ 이동은 pending 크기를 N이라 할 때 O(log N),
한 배치의 응답 공간은 배치 크기 C에 대해 O(C)다. payload 크기와 실제 후처리 비용은 별도다.

## 검증

```bash
# Redis가 실행 중이어야 한다. 기본 실행에 통합 테스트를 포함하고 DB 15에 샘플 키를 생성한다.
mise exec -- ./gradlew test
# usecase 통합 테스트만 제외할 때
RUN_REDIS_USECASE_TESTS=false mise exec -- ./gradlew test
```

일반 단위 테스트는 입력 제한, 세션 만료 시 쓰기 차단, 처리 실패 시 Stream ACK 차단을 검증한다.
Redis 통합 테스트는 기본 실행하며 `RUN_REDIS_USECASE_TESTS=false`일 때만 제외한다.
기존 contextLoads 테스트는 이 옵션과 무관하게 Redis가 필요하다. 통합 테스트는 캐시 중복 로딩 방어, TTL 갱신,
자료구조 동작, 재고 네 가지 모드, 큐 소비, Stream ACK와 HTTP 오류 응답을 검증한다.
테스트 DB 15도 다른 데이터가 없는 학습용 DB를 사용한다. 자동 FLUSH나 키 전체 삭제는 하지 않는다.
