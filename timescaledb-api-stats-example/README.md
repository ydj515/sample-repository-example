# timescaledb-api-stats-example

API 호출 이벤트를 Redis Stream에 적재하고 내부 consumer가 TimescaleDB hypertable에 저장한 뒤 통계 API로 조회하는 예제입니다.

## Stack

- Kotlin
- Spring Boot
- Gradle Kotlin DSL
- TimescaleDB: `timescale/timescaledb:2.28.2-pg17-oss`
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
curl "http://localhost:8080/api/stats/top-endpoints?from=$FROM&to=$TO&limit=10"
curl "http://localhost:8080/api/stats/clients?from=$FROM&to=$TO"
curl "http://localhost:8080/api/stats/auth-failures?from=$FROM&to=$TO"
```

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

## Redis Stream

```sh
docker compose exec redis redis-cli XLEN api-call-events
docker compose exec redis redis-cli XREVRANGE api-call-events + - COUNT 5
docker compose exec redis redis-cli XPENDING api-call-events api-call-event-writers
```

### Stream 접근 구조

Redis Stream 접근은 도메인 인터페이스 `ApiCallEventStreamRepository`로 일원화하고, `StringRedisTemplate` 기반 구현체 `RedisApiCallEventStreamRepository`가 XADD/XREADGROUP/XACK/XTRIM/XPENDING/XCLAIM을 담당합니다. publisher와 consumer는 `StringRedisTemplate`을 직접 다루지 않고 이 인터페이스에만 의존합니다.

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

API 통계 dashboard는 Grafana PostgreSQL datasource가 `api_call_events` hypertable을 직접 조회합니다. JVM/DB 운영 dashboard는 Prometheus datasource를 통해 Spring Boot actuator와 PostgreSQL exporter metric을 조회합니다.

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

## Reset

init SQL은 Docker volume 최초 생성 시에만 실행됩니다. 스키마나 seed를 다시 적용하려면 volume을 삭제합니다.

```sh
docker compose down -v
docker compose up -d
```
