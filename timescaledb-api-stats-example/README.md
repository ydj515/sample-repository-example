# timescaledb-api-stats-example

API 호출 이벤트를 Redis Stream에 적재하고 내부 consumer가 TimescaleDB hypertable에 저장한 뒤 통계 API로 조회하는 예제입니다.

수집 파이프라인(요청 → Redis Stream → consumer → hypertable)을 갖춘 뒤, **그 위에 쌓인 시계열을 어떻게 가공할 것인가**를 다룹니다. 요약 테이블을 배치로 직접 만드는 방법도 있고, TimescaleDB의 continuous aggregate에 위임하는 방법도 있습니다. 이 예제는 후자를 구현하되 `/api/stats/api-key-calls?source=raw|aggregate`로 두 방식을 같은 응답 스키마에서 비교할 수 있게 해 뒀습니다. 자세한 비교는 [집계를 어디서 할 것인가](#집계를-어디서-할-것인가-source)를 참고하세요.

## Stack

- Kotlin
- Spring Boot
- Gradle Kotlin DSL
- TimescaleDB: `timescale/timescaledb:2.28.2-pg17`
- Redis: `redis:7.2-alpine`
- Grafana: `grafana/grafana:11.5.2`
- Prometheus: `prom/prometheus:v2.48.0`
- PostgreSQL exporter: `prometheuscommunity/postgres-exporter:v0.17.1`
- k6

## Architecture

```text
request
  -> Spring Boot API
  -> ApiCallCaptureFilter
  -> ApiKeyAuthFilter
  -> Redis Stream
  -> internal consumer
  -> TimescaleDB api_call_events hypertable
  -> Stats API

Grafana PostgreSQL datasource
  -> TimescaleDB api_call_events hypertable
  -> API statistics dashboard

Prometheus datasource
  -> Spring Boot /actuator/prometheus
  -> PostgreSQL exporter
  -> JVM and DB operations dashboard
```

## Package Structure

레이어드 아키텍처(4계층)로 구성합니다. 의존 방향은 presentation -> application -> domain 이며, infrastructure가 domain의 port를 구현합니다.

```text
com.example.timescaledbapistatsexample
├── presentation      # controller, filter, filter config
├── application       # ApiAccessService, ApiCallEventPublisher, ApiCallEventConsumer, StreamEntryParser
├── domain
│   ├── model         # ApiClient, ApiCallEvent, ApiCallEventRecord, StreamEntry, StatsModels ...
│   ├── port          # ApiAccessSnapshotProvider, ApiCallEventStreamRepository, ApiCallEventStore, ApiStatsReader
│   └── service       # RoutePatternMatcher, Sha256ApiKeyHasher (순수 도메인 로직)
├── infrastructure
│   ├── redis         # RedisApiCallEventStreamRepository (StringRedisTemplate 어댑터)
│   └── persistence   # ApiAccessRepository, JdbcApiCallEventStore, ApiStatsRepository (JDBC 어댑터)
└── support           # 공용 유틸(예: Throwable.hasMessageInChain)
```

- Redis/JDBC 접근은 `domain.port` 인터페이스 뒤로 숨기고, `StringRedisTemplate`은 `infrastructure.redis` 한 곳에서만 사용합니다.
- `application`은 `infrastructure`를 직접 참조하지 않고 port에만 의존하므로, 저장소 구현을 교체해도 상위 계층은 바뀌지 않습니다.

## Run

```sh
docker compose up -d
./gradlew bootRun
```

Services:

- Spring Boot: <http://localhost:8080>
- Grafana: <http://localhost:3000> (`admin` / `admin`)
- Prometheus: <http://localhost:9090>
- PostgreSQL exporter: <http://localhost:9187/metrics>
- TimescaleDB: `localhost:5432`
- Redis: `localhost:6379`

## Demo API Keys

```text
demo-key-client-01
demo-key-client-02
demo-key-client-03
demo-key-client-04
demo-key-client-05
demo-key-client-06
demo-key-client-07
demo-key-client-08
demo-key-client-09
demo-key-client-10
```

## Sample Calls

```sh
curl -H "X-API-Key: demo-key-client-01" http://localhost:8080/api/products
curl -H "X-API-Key: demo-key-client-10" http://localhost:8080/api/reports/sales
curl http://localhost:8080/api/products
```

## k6

```sh
brew install k6
k6 run k6/api-stats.js
```

Docker로 실행하려면 다음 명령을 사용할 수 있습니다.

```sh
docker run --rm \
  -e BASE_URL=http://host.docker.internal:8080 \
  -v "$PWD/k6:/scripts:ro" \
  grafana/k6:latest run /scripts/api-stats.js
```

## Stats API

```sh
FROM=$(date -u -v-1H +"%Y-%m-%dT%H:%M:%SZ")
TO=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

curl "http://localhost:8080/api/stats/calls?bucket=1%20minute&from=$FROM&to=$TO"
curl "http://localhost:8080/api/stats/latency?bucket=1%20minute&from=$FROM&to=$TO"
curl "http://localhost:8080/api/stats/failure-rate?bucket=1%20minute&from=$FROM&to=$TO"
curl "http://localhost:8080/api/stats/api-key-calls?period=day&from=$FROM&to=$TO&limit=100"
curl "http://localhost:8080/api/stats/top-endpoints?from=$FROM&to=$TO&limit=10"
curl "http://localhost:8080/api/stats/clients?from=$FROM&to=$TO"
curl "http://localhost:8080/api/stats/auth-failures?from=$FROM&to=$TO"
```

`/api/stats/api-key-calls`는 `period=day|month|year`를 지원하고, `apiClientId`, `method`, `pathPattern`, `limit` 필터를 선택적으로 받을 수 있습니다. API Key 원문은 저장하지 않으므로 결과는 `apiClientId`와 `apiClientName` 기준으로 집계됩니다.

### 집계를 어디서 할 것인가 (`source`)

같은 통계를 두 경로로 뽑아 비교할 수 있습니다. 응답 스키마가 동일하므로 결과와 응답 시간을 그대로 대조할 수 있습니다.

```sh
# 조회 시점에 hypertable을 훑는다 (배치/온디맨드로 직접 가공하는 방식과 같은 비용 구조)
curl "http://localhost:8080/api/stats/api-key-calls?period=day&source=raw&from=$FROM&to=$TO"

# 미리 말아둔 continuous aggregate를 읽는다 (가공을 TimescaleDB에 위임)
curl "http://localhost:8080/api/stats/api-key-calls?period=day&source=aggregate&from=$FROM&to=$TO"
```

`source`의 기본값은 `aggregate`입니다.

조회 구간은 `period` 단위의 **버킷 경계로 스냅**됩니다. raw는 이벤트 시각(`occurred_at`)에, 집계 view는 버킷 시작 시각(`bucket`)에 필터가 걸리기 때문입니다. 예를 들어 `period=month`에서 7월 버킷의 라벨은 `2026-07-01`인데, `from=2026-07-18`로 그대로 필터를 걸면 원본 이벤트는 구간 안에 있어도 버킷 라벨이 구간 밖이라 집계 쪽 결과만 비어버립니다. 양쪽 모두 "구간에 걸친 완전한 버킷"을 대상으로 맞춰 두 경로의 결과가 일치합니다.

| | `source=raw` | `source=aggregate` |
|---|---|---|
| 집계 시점 | 조회할 때마다 | 백그라운드에서 미리 |
| 조회 비용 | 구간이 넓어질수록 선형 증가 | 구간과 거의 무관 |
| 최신성 | 항상 최신 | realtime aggregation으로 최신 |
| 원본 삭제 후 | 조회 불가 | 계속 조회 가능 |
| 직접 관리할 것 | 배치 스케줄, 재처리, 멱등성, 백필 | 없음 (정책 선언만) |

이 예제가 말하려는 지점이 바로 이 표입니다. 요약 테이블을 배치로 직접 만들어도 결과는 같지만, 그 경우 스케줄링·실패 재처리·백필·중복 방지를 전부 직접 짜야 합니다. Continuous aggregate는 그 책임을 선언 하나로 DB에 넘기고, invalidation 추적과 증분 갱신을 엔진이 처리합니다.

## SQL

```sh
docker compose exec timescaledb psql -U api_stats -d api_stats
```

```sql
SELECT time_bucket('1 minute', occurred_at) AS bucket, count(*) AS total_calls
FROM api_call_events
GROUP BY bucket
ORDER BY bucket;

SELECT method, path_pattern, count(*) AS total_calls
FROM api_call_events
GROUP BY method, path_pattern
ORDER BY total_calls DESC;
```

### Continuous aggregate (계층형)

일/월/년 집계를 각각 raw에서 다시 계산하지 않고, 한 단계 아래 집계를 롤업합니다.

```text
api_call_events (raw hypertable)
  -> api_key_call_stats_daily     (raw를 읽는 유일한 단계)
  -> api_key_call_stats_monthly   (daily를 롤업)
  -> api_key_call_stats_yearly    (monthly를 롤업)
```

raw를 스캔하는 것은 daily 하나뿐이라 상위 집계의 갱신 비용이 크게 줄어듭니다.

```sql
SELECT bucket, api_client_name, method, path_pattern, total_calls
FROM api_key_call_stats_daily
ORDER BY bucket DESC, total_calls DESC
LIMIT 20;
```

집계 view는 평균 대신 `total_duration_ms`(합계)와 `total_calls`(건수)를 저장합니다. 평균을 그대로 저장하면 상위 단계에서 `avg(average_duration_ms)`가 "평균의 평균"이 되어 값이 틀어지기 때문입니다. 합계와 건수를 들고 있으면 어느 단계에서든 정확한 가중평균을 복원할 수 있습니다.

```sql
SELECT bucket, sum(total_duration_ms)::numeric / sum(total_calls) AS avg_ms
FROM api_key_call_stats_monthly
GROUP BY bucket;
```

> 롤업 view의 `GROUP BY`는 반드시 위치 참조(`GROUP BY 1, 2, ...`)로 써야 합니다. 출력 별칭 `bucket`과 원본 컬럼 `bucket`의 이름이 같아서 `GROUP BY bucket`이라고 쓰면 Postgres가 원본 컬럼으로 해석하고, TimescaleDB가 `continuous aggregate view must include a valid time bucket function` 오류를 냅니다.

### realtime aggregation

TimescaleDB 2.13+ 는 신규 continuous aggregate를 `materialized_only = true`로 만듭니다. 이 상태에서는 아직 materialize 되지 않은 최신 구간이 조회 결과에서 통째로 빠지기 때문에, k6를 1분 돌리고 대시보드를 열면 CAGG 패널이 빈 화면이 됩니다.

그래서 세 view 모두 realtime aggregation을 켜 둡니다. "materialize된 과거 + 실시간 계산한 최근"을 합쳐서 돌려주므로 방금 들어온 이벤트도 즉시 보입니다.

```sql
SELECT view_name, materialized_only FROM timescaledb_information.continuous_aggregates;
```

> **materialization watermark**
>
> realtime aggregation이 메꿔주는 것은 watermark **이후** 구간뿐입니다. watermark는 마지막으로 materialize된 지점이고, `refresh_continuous_aggregate(view, NULL, NULL)` 같은 전체 refresh를 돌리면 현재 시점까지 올라갑니다.
>
> 그 뒤에 watermark보다 **과거** 시각으로 데이터를 넣으면(늦게 도착한 이벤트, 과거 백필) 다음 refresh가 돌기 전까지 집계에 잡히지 않습니다. 운영에서는 정책의 `end_offset`(daily 기준 1분)만큼 watermark가 항상 현재보다 뒤처져 있어서 새 이벤트가 언제나 watermark 이후에 놓이므로 문제가 되지 않습니다. 백필 직후라면 `refresh_continuous_aggregate`를 한 번 더 돌리거나 정책 주기를 기다리면 됩니다.

### 압축 / 보존

집계를 CAGG에 위임했으므로 raw 이벤트는 최근 조회와 재집계용으로만 있으면 됩니다. 7일이 지난 청크는 압축하고, 180일이 지나면 삭제합니다. 원본이 사라져도 통계는 집계 view에 남습니다.

```sql
SELECT pg_size_pretty(sum(before_compression_total_bytes)) AS before,
       pg_size_pretty(sum(after_compression_total_bytes))  AS after
FROM hypertable_compression_stats('api_call_events');

SELECT proc_name, hypertable_name, schedule_interval
FROM timescaledb_information.jobs
WHERE proc_name IN ('policy_compression', 'policy_retention', 'policy_refresh_continuous_aggregate');
```

> 압축 청크에는 `INSERT ... ON CONFLICT`가 제약을 받습니다. 이 예제의 쓰기는 항상 현재 시각이라 압축 대상(7일 경과) 청크에 닿지 않지만, 과거 시점 이벤트를 백필한다면 해당 청크를 먼저 `decompress_chunk`로 풀어야 합니다.

## 과거 데이터 백필

k6는 "지금" 트래픽만 만들기 때문에 압축, 보존, 월/년 집계, raw와 aggregate의 성능 차이가 드러나지 않습니다. 과거 구간을 채우는 스크립트를 따로 두었습니다.

```sh
docker compose exec -T timescaledb psql -U api_stats -d api_stats \
  < infra/timescaledb/seed/backfill_history.sql
```

기본값은 400일 x 2000건(80만 건)이고, 규모를 바꾸려면 변수를 넘깁니다.

```sh
docker compose exec -T timescaledb psql -U api_stats -d api_stats \
  -v days=800 -v events_per_day=5000 < infra/timescaledb/seed/backfill_history.sql
```

스크립트는 이벤트를 생성한 뒤 집계를 계층 순서대로 materialize 하고, 7일이 지난 청크를 압축한 다음 결과를 출력합니다. 실제 실행 결과(80만 건 기준)입니다.

```text
 raw_events | daily_rows | monthly_rows | yearly_rows
------------+------------+--------------+-------------
     800000 |      24000 |          840 |         120

 total_chunks | compressed
--------------+------------
          400 |        393

 before  | after  | saved_pct
---------+--------+-----------
 365 MB  | 106 MB |      70.8
```

같은 데이터에서 연 단위 집계를 뽑을 때 두 방식의 차이입니다.

| 조회 대상 | Execution Time |
|---|---|
| `api_call_events` (raw, `time_bucket('1 year', ...)`) | 4485 ms |
| `api_key_call_stats_yearly` (continuous aggregate) | 0.1 ms |

배치로 요약 테이블을 직접 만들었어도 조회 시간은 비슷하게 빨라집니다. 차이는 그 요약 테이블을 누가 관리하느냐입니다.

> **백필 스크립트가 refresh 범위를 끝까지 주지 않는 이유**
>
> `refresh_continuous_aggregate(view, NULL, NULL)`처럼 끝을 열어두면 현재 월/년 버킷까지 materialize 되면서 watermark가 그 버킷 너머로 올라갑니다. 그러면 realtime aggregation이 메꿔줄 구간이 사라져, 백필 이후 새로 들어오는 이벤트가 다음 정책 refresh(월 10분, 년 1시간)까지 월/년 집계에 잡히지 않습니다.
>
> 그래서 스크립트는 끝 경계를 `date_trunc('month', now())`로 잡아 현재 버킷을 일부러 비워둡니다. 덕분에 백필 직후 라이브 트래픽을 보내도 `source=raw`와 `source=aggregate`가 즉시 일치합니다.
>
> 같은 이유로 refresh policy에도 `end_offset`이 있습니다. 정책이 현재 구간을 건드리지 않고 남겨두면 realtime aggregation이 그 구간을 담당합니다.
>
> 상위 집계는 하위 집계의 **materialize된** 데이터를 읽으므로, 수동 refresh는 반드시 daily → monthly → yearly 순서로 해야 합니다.

## Test

```sh
./gradlew test
```

단위 테스트와 함께 Testcontainers 기반 통합 테스트가 실행됩니다. 통합 테스트는 운영과 같은 `timescale/timescaledb:2.28.2-pg17` 이미지를 띄우고 `infra/timescaledb/init`의 init SQL을 그대로 마운트하므로, 스키마가 깨지면 바로 실패합니다.

검증 대상:

- realtime aggregation이 꺼지면 최신 구간이 빠지는지 (`materialized_only`)
- `source=raw`와 `source=aggregate`가 day/month/year 전부에서 같은 결과를 주는지 (버킷 경계 스냅)
- 계층형 롤업이 가중평균을 정확히 복원하는지 (평균의 평균 방지)
- `limit`이 버킷별 상위 N개로 적용되는지
- 시간 인덱스가 중복 생성되지 않는지
- 월/년 집계가 raw가 아니라 한 단계 아래 집계를 읽는지
- 압축 후에도 집계 값이 유지되는지
- 같은 `stream_id` 재저장이 멱등한지

Docker를 쓸 수 없는 환경에서는 통합 테스트가 skip 되고 이유가 콘솔에 출력됩니다.

> 최신 Docker 엔진(OrbStack, Docker 25+)은 API 1.40 이상을 요구하는데 docker-java 기본값은 1.32라 `client version 1.32 is too old`로 붙지 못합니다. `build.gradle.kts`의 test task에서 `api.version`을 1.44로 지정해 두었고, 다른 값이 필요하면 `DOCKER_API_VERSION` 환경변수로 덮어쓸 수 있습니다.

## Redis Stream

```sh
docker compose exec redis redis-cli XLEN api-call-events
docker compose exec redis redis-cli XREVRANGE api-call-events + - COUNT 5
docker compose exec redis redis-cli XPENDING api-call-events api-call-event-writers
```

### Stream 접근 구조

Redis Stream 접근은 도메인 인터페이스 `ApiCallEventStreamRepository`로 일원화하고, `StringRedisTemplate` 기반 구현체 `RedisApiCallEventStreamRepository`가 batch XADD/XREADGROUP/XACK/XTRIM/XPENDING/XCLAIM을 담당합니다. publisher와 consumer는 `StringRedisTemplate`을 직접 다루지 않고 이 인터페이스에만 의존합니다.

### 비동기 발행 (bounded queue + batch XADD)

요청 스레드는 Redis I/O를 직접 수행하지 않습니다. `ApiCallEventPublisher`가 이벤트를 bounded 인메모리 큐에 offer만 하고(논블로킹), 전용 워커 스레드가 큐를 배치로 비워 파이프라인 XADD(왕복 1회)로 발행합니다. 이렇게 하면 Redis 지연/장애가 요청 지연으로 전파되지 않습니다.

- 큐가 가득 차면 이벤트를 drop 하고 카운트만 올립니다(텔레메트리가 본 트래픽을 밀어내지 않도록 하는 명시적 backpressure).
- 정상 종료(`@PreDestroy`) 시 남은 큐를 flush 해 유실을 막습니다. 단 인메모리 버퍼이므로 하드킬(SIGKILL/정전) 시에는 아직 발행되지 않은 이벤트가 유실될 수 있습니다.

관련 설정(`application.yml`):

```yaml
api-stats:
  publisher:
    queue-capacity: 10000  # bounded 큐 크기(초과분은 drop)
    batch-size: 500        # 한 번의 파이프라인 XADD로 묶는 최대 개수
    poll-timeout-ms: 200   # 워커가 큐를 기다리는 최대 시간
```

### Pending 메시지 회수 (XPENDING + XCLAIM)

consumer가 메시지를 전달받은 뒤 ACK 이전에 죽으면 해당 메시지는 pending 상태로 남습니다. `ApiCallEventConsumer.reclaimStalePending()`이 주기적으로 idle 시간이 임계값을 넘긴 pending 메시지를 `XPENDING`으로 찾아 `XCLAIM`으로 회수한 뒤 재저장합니다. 재저장 중복은 `(stream_id, occurred_at)` primary key로 흡수됩니다.

관련 설정(`application.yml`):

```yaml
api-stats:
  consumer:
    reclaim:
      enabled: true       # 회수 스케줄러 on/off
      min-idle-ms: 60000  # 이 시간 이상 idle인 pending만 회수(정상 처리 중 메시지 탈취 방지)
      batch-size: 100     # 한 회수 주기당 처리 개수
      fixed-delay-ms: 30000
```

회수 동작은 앱 실행 중 pending을 강제로 만든 뒤 확인할 수 있습니다.

```sh
docker compose exec redis redis-cli XPENDING api-call-events api-call-event-writers
# min-idle-ms 경과 후 로그에 "Reclaimed N stale pending Redis Stream entries" 출력, XPENDING 개수 감소
```

## Grafana Dashboards

Grafana는 provisioning으로 datasource와 dashboard를 자동 등록합니다.

- `Prometheus`: JVM, HTTP, PostgreSQL exporter 운영 지표용 datasource
- `TimescaleDB API Stats`: API 호출 이벤트 분석용 PostgreSQL datasource
- `API Stats - TimescaleDB`: TimescaleDB를 직접 조회하는 API 통계 dashboard
- `Operations - Prometheus`: Prometheus를 조회하는 JVM/DB 운영 dashboard

API 통계 dashboard는 Grafana PostgreSQL datasource가 `api_call_events` hypertable과 continuous aggregate view를 직접 조회합니다. JVM/DB 운영 dashboard는 Prometheus datasource를 통해 Spring Boot actuator와 PostgreSQL exporter metric을 조회합니다.

`API Stats - TimescaleDB` dashboard에는 raw hypertable과 continuous aggregate를 비교하는 패널이 분리되어 있습니다.

- `Raw API Key Calls - Daily` / `Continuous Aggregate API Key Calls - Daily`
- `Raw API Key Calls - Monthly` / `Continuous Aggregate API Key Calls - Monthly`
- `Raw API Key Calls - Yearly` / `Continuous Aggregate API Key Calls - Yearly`

월/년 패널은 버킷 타임스탬프가 해당 월/년의 1일이라 대시보드 기본 시간범위(`now-1h`)에는 잡히지 않습니다. 패널 단위로 `timeFrom`(2년 / 5년)을 지정해 시간범위와 무관하게 보이도록 해 두었습니다.

### Storage - Compression & Chunks

압축과 청크 상태를 보는 패널이 별도 row로 있습니다.

- `압축 전 크기` / `압축 후 크기` / `절감률` — `hypertable_compression_stats()`
- `청크 (전체 / 압축됨)` — `timescaledb_information.chunks`
- `청크 상세 (최근 20개)` — 청크별 시간 범위, 압축 여부, 크기
- `정책 / 백그라운드 job` — refresh / 압축 / 보존 정책의 마지막 실행 상태

압축은 7일이 지난 청크부터 적용되므로, k6만 돌린 직후에는 압축된 청크가 0입니다. [과거 데이터 백필](#과거-데이터-백필)을 먼저 실행하면 수치가 채워집니다.

기존 Docker volume이 이미 생성된 상태라면 새 continuous aggregate init SQL은 자동 적용되지 않습니다. 비교 패널을 보려면 `docker compose down -v && docker compose up -d`로 volume을 다시 만들거나, `infra/timescaledb/init/01_schema.sql`의 continuous aggregate SQL을 TimescaleDB에 수동 적용해야 합니다.

Spring Boot actuator metric을 Prometheus가 수집하려면 앱이 실행 중이어야 합니다.

```sh
./gradlew bootRun
curl http://localhost:8080/actuator/prometheus
```

Prometheus scrape target 상태는 다음 URL에서 확인합니다.

```text
http://localhost:9090/targets
```

Grafana datasource provisioning 파일:

```text
infra/grafana/provisioning/datasources/datasources.yml
infra/grafana/provisioning/dashboards/dashboards.yml
```

Dashboard JSON:

```text
infra/grafana/dashboards/api-stats-timescaledb.json
infra/grafana/dashboards/operations-prometheus.json
```

## 예제로서의 의도적인 단순화

예제라서 일부러 단순하게 둔 부분입니다. 실제 서비스에 옮길 때는 그대로 쓰면 안 됩니다.

- **`/api/stats/*`는 인증이 없습니다.** 통계 조회를 캡처하면 통계가 다시 통계 이벤트를 만드는 순환이 생겨 필터에서 제외했고, 그 결과 client 이름과 호출량이 인증 없이 노출됩니다. 실제로는 별도 관리자 인증을 붙여야 합니다.
- **`api_routes`에 없는 경로는 API Key 없이 통과합니다(default-allow).** 등록된 route만 인가 대상으로 보는 구조라, 오타 경로나 신규 엔드포인트가 무인증으로 열립니다. 운영에서는 default-deny가 맞습니다.
- **`client_ip`와 `user_agent`를 원본 그대로 저장합니다.** 개인정보 취급 기준에 따라 마스킹하거나 보존 기간을 따로 짧게 잡아야 할 수 있습니다.
- **API Key가 평문으로 README와 seed에 들어 있습니다.** 데모용이며, 저장은 SHA-256 해시로만 합니다.

## Reset

init SQL은 Docker volume 최초 생성 시에만 실행됩니다. 스키마나 seed를 다시 적용하려면 volume을 삭제합니다.

```sh
docker compose down -v
docker compose up -d
```
