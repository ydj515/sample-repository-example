# WebFlux with Redis Example

Spring WebFlux와 Redis 자료구조를 HTTP API로 학습하는 예제다.
명령어별 Router, 캐시 전략, 상품·장바구니·인기 지표·재고·주문 이벤트 시나리오를 제공한다.

## 실행

Java 21, Gradle Wrapper 8.14.5, Docker Compose가 필요하다. mise를 사용하면 `mise.toml`의 JDK를 적용한다.

```bash
mise install
docker compose up -d
SPRING_DATA_REDIS_PASSWORD= mise exec -- ./gradlew bootRun
```

기본 주소는 `http://localhost:8080`, Redis는 `localhost:6379`다.
Compose의 Redis는 인증을 사용하지 않으므로 실행 명령에서 비밀번호를 빈 값으로 지정한다.

mise 없이 실행하면 Java 21을 `JAVA_HOME`에 지정하고 `SPRING_DATA_REDIS_PASSWORD= ./gradlew bootRun`을 사용한다.

## 예제 실행

IntelliJ HTTP Client에서 `http/*.http`를 열고 개별 요청을 실행한다.
세션과 복구 커서는 응답 스크립트로 자동 저장한다. 다른 클라이언트에서는 응답 값을 직접 입력한다.

| 예제 | 요청 파일 |
|---|---|
| 자료구조별 명령어 | `http/string.http`, `hash.http`, `list.http`, `set.http`, `sortedSet.http`, `pubsub.http`, `stream.http`, `lua.http` |
| 캐시 전략 | [strategy.http](http/strategy.http) |
| 상품 캐시와 중복 로딩 방어 | [usecase-product.http](http/usecase-product.http) |
| Hash 장바구니와 슬라이딩 세션 만료 | [usecase-cart.http](http/usecase-cart.http) |
| 찜·랭킹·방문 집계 | [usecase-popularity.http](http/usecase-popularity.http) |
| 재고 동시성 비교 | [usecase-inventory.http](http/usecase-inventory.http) |
| 주문 이벤트 발행과 소비 | [usecase-order-events.http](http/usecase-order-events.http) |
| Stream 재시도 제한과 DLQ | [usecase-stream-dlq.http](http/usecase-stream-dlq.http) |
| 중단된 Stream 소비자 복구 | [usecase-stream-recovery.http](http/usecase-stream-recovery.http) |

복구 예제는 발행 → 그룹 생성 → ACK 없는 소비 → pending 확인 → idle 이후 회수 → 처리/ACK 순서다.
전체 파일 일괄 실행 대신 번호 순서대로 실행한다. Pub/Sub SSE는 연결을 유지한 채 별도 요청으로 발행한다.

## 기본 Redis CLI 예제

Compose Redis에 접속한 뒤 아래 명령을 한 줄씩 실행한다. CLI 예제는 `cli:` 키를 사용한다.

```bash
docker exec -it redis-example redis-cli
```

| 자료구조 | 기본 명령 예시 | HTTP 예제 |
|---|---|---|
| String · TTL | `SET cli:views 10 EX 300`, `GET cli:views`, `INCRBY cli:views 5`, `TTL cli:views` | [string.http](http/string.http) |
| Hash | `HSET cli:cart product-1 2`, `HGET cli:cart product-1`, `HGETALL cli:cart`, `EXPIRE cli:cart 1800` | [hash.http](http/hash.http) |
| List | `RPUSH cli:queue order-1 order-2`, `LRANGE cli:queue 0 -1`, `LPOP cli:queue` | [list.http](http/list.http) |
| Set | `SADD cli:wishlist product-1 product-2`, `SMEMBERS cli:wishlist`, `SISMEMBER cli:wishlist product-1` | [set.http](http/set.http) |
| ZSet | `ZADD cli:ranking 10 product-1 20 product-2`, `ZINCRBY cli:ranking 5 product-1`, `ZREVRANGE cli:ranking 0 9 WITHSCORES` | [sortedSet.http](http/sortedSet.http) |
| HyperLogLog | `PFADD cli:visitors user-1 user-2`, `PFCOUNT cli:visitors` | [usecase-popularity.http](http/usecase-popularity.http) |
| Bitmap | `SETBIT cli:visited 42 1`, `GETBIT cli:visited 42`, `BITCOUNT cli:visited` | [usecase-popularity.http](http/usecase-popularity.http) |

List의 pop은 항목을 꺼내고 제거한다. `TTL`의 `-1`은 만료 없음, `-2`는 키 없음이다.
HyperLogLog는 고유 수 추정치이며 Bitmap offset은 작은 정수로 사용한다.

### Pub/Sub

CLI 연결 두 개를 열어 첫 번째에서 구독하고, 두 번째에서 발행한다.
구독 연결은 메시지를 계속 기다리므로 별도 터미널을 사용한다.

```text
SUBSCRIBE cli:notifications
```

```text
PUBLISH cli:notifications "order-created"
```

HTTP에서도 [pubsub.http](http/pubsub.http)의 SSE 구독을 먼저 실행한 뒤 발행한다.

### Stream 소비자 그룹

```text
XADD cli:orders * productId 1 quantity 2
XGROUP CREATE cli:orders cli-workers 0-0
XREADGROUP GROUP cli-workers worker-1 COUNT 1 STREAMS cli:orders >
XPENDING cli:orders cli-workers
```

XADD/XREADGROUP 응답의 실제 레코드 ID를 넣어 `XACK cli:orders cli-workers 실제레코드ID`를 실행한다.
같은 그룹을 다시 생성하면 BUSYGROUP 오류가 발생한다. ACK는 pending에서 제거하며 Stream 레코드는 보관한다.
HTTP 실행 흐름은 [stream.http](http/stream.http)를 참고한다.

### Lua 원자 처리

```text
SET cli:stock 10
EVAL "local s=tonumber(redis.call('GET',KEYS[1])); local q=tonumber(ARGV[1]); if not s or s<q then return -1 end; return redis.call('DECRBY',KEYS[1],q)" 1 cli:stock 3
GET cli:stock
```

결과는 7이며, 같은 감소 요청을 반복해 재고가 부족해지면 -1을 반환한다.
HTTP에서는 [lua.http](http/lua.http)로 스크립트 실행과 SHA 기반 실행을 확인한다.

## 캐시 전략 예제

[strategy.http](http/strategy.http)에서 Cache-aside, Write-through, Read-through, Write-behind,
PER, Lock, Background refresh를 실행한다.
Fake DB와 Redis 캐시를 사용하며, 기본 TTL은 5분이다.
전략별 처리 순서·실행 예시·응답·보장 한계는 [캐시 전략 문서](docs/strategy.md)를 참고한다.

## 구조

`presentation/router → application → infrastructure/repository` 흐름을 사용한다.
업무 예제는 각 계층의 `usecase/{product,cart,popularity,inventory,orderevent}`에 둔다.
Stream 소비 코드는 `infrastructure/consumer/usecase/orderevent`, 원자 처리 스크립트는 `resources/lua/usecase`에 있다.

API, TTL, 입력 제한, 전달 보장 범위는 [업무 시나리오 문서](docs/usecases.md)를 참고한다.
학습용 Fake DB와 수동 소비 API이며, 자동 워커·외부 주문 처리·멱등성을 구현하지 않는다.
Stream 처리 실패는 기본 3회(첫 처리 포함)까지 허용하며, 한도 도달 시 DLQ로 이동하고 원본을 ACK한다.
`app.usecase.order-stream.max-attempts`로 1..10 범위에서 설정한다.

## 검증

```bash
# 로컬 Redis 필요. 기본적으로 전체 테스트를 실행하며 DB 15에 테스트 데이터를 생성한다.
mise exec -- ./gradlew build
# usecase 통합 테스트만 제외할 때
RUN_REDIS_USECASE_TESTS=false mise exec -- ./gradlew test
```

환경변수를 지정하지 않으면 usecase 통합 테스트를 실행한다. `false`는 해당 테스트만 제외하며,
기존 `contextLoads` 테스트도 Redis 연결이 필요하다. Redis 없이 단위 테스트만 실행하려면
`mise exec -- ./gradlew test --tests "*UsecaseValidationTest"`를 사용한다.

단위 테스트는 입력 제한과 실패 시 ACK 차단을 검증한다.
통합 테스트는 캐시, 만료, 재고 경쟁, 이벤트 소비, pending 회수 및 HTTP 오류 응답을 검증한다.
운영 데이터와 분리한 로컬 Redis에서 실행한다.
