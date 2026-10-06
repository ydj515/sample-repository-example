# Redis 캐시 전략

이 문서는 현재 `/strategy` 구현의 처리 순서와 실행 방법을 설명한다.
앱 실행 방법은 [README](../README.md), 전체 요청은 [strategy.http](../http/strategy.http)를 참고한다.

## 공통 조건

- 원본은 `StrategyUserFakeDbRepository`의 메모리 저장소다. 없는 ID를 조회하면 기본 사용자를 생성한다.
- 캐시 키는 `strategy:user:{userId}`, TTL은 `app.redis.default-ttl`의 기본값 5분이다.
- 전략들이 같은 사용자 캐시를 공유하므로 한 전략에서 저장한 값이 다른 전략 조회에도 사용된다.
- GET은 `?userId=100`, PUT은 `userId`, `name`, `email`, `age`가 담긴 JSON을 사용한다.
- Fake DB는 재시작하면 초기화되지만 Redis의 캐시와 큐는 남을 수 있다.

처리 흐름은 `StrategyRouter → StrategyHandler → RedisStrategyService → StrategyRepository/Fake DB`다.
이는 실제 DB 연동이나 운영 성능 검증을 위한 구성이 아니다.

## 전략과 API

| 전략 | 처리 순서 | API |
|---|---|---|
| Cache-aside | 조회: 캐시 → miss 시 DB → 캐시 적재. 수정: DB 저장 → 캐시 무효화 | GET/PUT `/strategy/cache-aside/user` |
| Write-through | 수정: 캐시 저장 → DB 저장. 조회 miss: DB → 캐시 적재 | GET/PUT `/strategy/write-through/user` |
| Read-through | 캐시 → miss 시 Service의 loader 경로 → DB → 캐시 적재 | GET `/strategy/read-through/user` |
| Write-behind | 캐시 저장 → List 큐 등록 → 별도 소비 요청에서 DB 반영 | PUT `/strategy/write-behind/user` |
| PER | cache hit 시 남은 TTL로 확률 계산 → 선택된 요청이 DB 조회·캐시 갱신 | GET `/strategy/per/user` |
| Lock | cache miss 시 락 획득 → DB 조회 → 캐시 적재 → 락 해제 | GET `/strategy/lock/user` |
| Background refresh | 사용자 목록 → 순차 DB 조회 → 캐시 갱신 | POST `/strategy/background-refresh` |

원본 저장소 조회/저장은 GET/PUT `/strategy/db/user`, 큐 소비는 POST `/strategy/write-behind/process-next`,
캐시 값과 TTL 조회는 GET `/strategy/cache/value-with-ttl?key=strategy:user:100`을 사용한다.
원본 저장 API는 캐시를 무효화하지 않는다.

## Cache-aside 실행

새 사용자 ID로 원본 저장 → 두 번 조회 → TTL 확인 순서로 실행한다.

```bash
curl -X PUT localhost:8080/strategy/db/user \
  -H 'Content-Type: application/json' \
  -d '{"userId":9201,"name":"sample-user","email":"sample@example.com","age":30}'
curl 'localhost:8080/strategy/cache-aside/user?userId=9201'
curl 'localhost:8080/strategy/cache-aside/user?userId=9201'
curl 'localhost:8080/strategy/cache/value-with-ttl?key=strategy:user:9201'
```

캐시가 없었다면 `source`는 첫 조회에서 `db`, 두 번째에서 `cache`다.
같은 ID에 캐시가 남아 있으면 첫 조회도 `cache`이므로 새로운 ID를 쓰거나 TTL 만료 후 비교한다.
`PUT /strategy/cache-aside/user`로 수정하면 DB를 저장하고 캐시를 무효화한다.
다음 GET 요청이 새 DB 값을 캐시에 적재한다.

## Write-through와 Read-through

[strategy.http](../http/strategy.http)의 Write-through PUT 후 GET을 실행한다.
현재 쓰기 순서는 캐시 저장 후 DB 저장이며, 성공 응답의 `source`는 `cache+db`다.
두 저장소를 하나의 트랜잭션으로 묶지 않아 DB 저장 실패 시 캐시만 반영될 수 있다.

Read-through는 Service 내부의 loader 예제로, 별도의 캐시 공급자 loader를 구성하지 않는다.
현재 Cache-aside 조회와 동일한 DB/캐시 적재 로직을 호출하지만 miss 응답의 `source`를 `loader`로 표시한다.

## Write-behind 실행

원본과 다른 값을 캐시·큐에 넣고, 소비 요청 전후의 원본을 비교한다.

```bash
curl -X PUT localhost:8080/strategy/db/user \
  -H 'Content-Type: application/json' \
  -d '{"userId":9203,"name":"before","email":"sample@example.com","age":30}'
curl -X PUT localhost:8080/strategy/write-behind/user \
  -H 'Content-Type: application/json' \
  -d '{"userId":9203,"name":"after","email":"sample@example.com","age":31}'
curl 'localhost:8080/strategy/db/user?userId=9203'
curl -X POST localhost:8080/strategy/write-behind/process-next
curl 'localhost:8080/strategy/db/user?userId=9203'
```

쓰기 응답의 `queued=true`는 큐 등록을 뜻하며 DB 반영 완료를 뜻하지 않는다.
큐는 `strategy:write_queue`에 RPUSH/LPOP으로 FIFO 처리하며 한 요청이 한 건만 소비한다.
앞선 작업이 있으면 목표 작업까지 소비 API를 반복해야 한다.
CLI에서는 `docker exec redis-example redis-cli LLEN strategy:write_queue`로 잔여 길이를 확인한다.
자동 워커·재시도·DLQ는 없고 pop 후 저장 실패 시 작업을 잃을 수 있다.
캐시 저장과 큐 등록도 하나의 원자 연산이 아니므로 큐 등록 실패 시 캐시만 반영될 수 있다.
Stream의 재시도·DLQ 예제는 [업무 시나리오 문서](usecases.md)에 별도로 제공한다.

## PER, 락, 갱신 실행

- PER: `GET /strategy/per/user?userId=100`을 반복한다. 캐시 miss는 `source=db-miss`,
  hit의 조기 재계산은 `source=db-recomputed`와 `refreshed=true`로 확인한다.
  `recomputeProbability`는 남은 TTL 감소 비율을 0..1로 제한한 단순 확률이다.
  TTL이 줄면 확률이 커지지만 매번 재계산되는 것은 아니며, 재계산 후 TTL은 다시 5분이다.
- Lock: `GET /strategy/lock/user?userId=104`를 캐시가 없는 상태에서 실행한다.
  락 키는 `lock:strategy:user:104`, lease는 10초다. 획득 실패 시 100ms 간격으로 최대 5번 재시도하고
  계속 실패하면 오류를 반환한다. 현재 구현은 락 획득 후 캐시를 다시 확인하지 않아 중복 DB 로딩을 완전히 막지 않는다.
- Background refresh: `POST /strategy/background-refresh`에 `{"userIds":[100,101]}`을 전달한다.
  `concatMap`으로 순차 갱신하고 완료된 사용자 목록을 반환한다. 빈 목록은 Fake DB에 저장된 전체 사용자를 대상으로 한다.
  API 이름과 달리 요청에 연결된 작업이며 응답 전에 완료를 기다린다. 자동 스케줄러는 없다.

## 응답 확인

| 필드 | 의미 |
|---|---|
| strategy / source | 적용 전략 / 응답 데이터 경로 |
| remainingTtlSeconds | 캐시의 남은 TTL 또는 적재 TTL. 캐시 무효화 응답 등에서는 null |
| refreshed | 로딩·조기 재계산 여부 |
| recomputeProbability | PER 재계산 확률. 다른 전략에서는 null |
| queued / queueSize | Write-behind 등록 여부 / 등록 후 큐 길이 |

TTL 전용 API는 `ValueWithTTL` 응답을 사용하며 전략 응답과 구조가 다르다.
Redis CLI의 `TTL strategy:user:100`으로 남은 초를 함께 확인할 수 있다.

## 보장 범위와 확인

- 제약: Fake DB는 JVM 내부 저장소이며 실제 DB의 트랜잭션·지연·장애를 재현하지 않는다.
- 위험: 캐시와 DB의 부분 반영, Write-behind 유실, 재시작 이후 원본과 캐시의 불일치가 가능하다.
- 예외: Redis 오류나 락 획득 실패는 요청 오류로 전파하며 DB fallback은 제공하지 않는다.

위 실행 예시는 코드의 처리 순서에 맞춰 작성한 수동 확인 절차다.
현재 업무 시나리오 통합 테스트가 각 `/strategy` API의 실행 성공을 증명하는 것은 아니다.
