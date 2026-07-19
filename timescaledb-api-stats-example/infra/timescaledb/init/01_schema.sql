CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS api_clients (
    id bigserial PRIMARY KEY,
    name text NOT NULL UNIQUE,
    api_key_hash text NOT NULL UNIQUE,
    enabled boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS api_routes (
    id bigserial PRIMARY KEY,
    method varchar(10) NOT NULL,
    path_pattern text NOT NULL,
    description text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (method, path_pattern)
);

CREATE TABLE IF NOT EXISTS api_client_route_permissions (
    api_client_id bigint NOT NULL REFERENCES api_clients(id),
    api_route_id bigint NOT NULL REFERENCES api_routes(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (api_client_id, api_route_id)
);

CREATE TABLE IF NOT EXISTS api_call_events (
    stream_id text NOT NULL,
    occurred_at timestamptz NOT NULL,
    api_client_id bigint,
    api_client_name text,
    auth_result text NOT NULL,
    denied_reason text,
    method text NOT NULL,
    path text NOT NULL,
    path_pattern text NOT NULL,
    status int NOT NULL,
    duration_ms bigint NOT NULL,
    client_ip text,
    user_agent text,
    error_type text,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (stream_id, occurred_at)
);

-- 쓰기가 잦은 이벤트 테이블이라 기본 7일보다 짧은 1일 청크를 쓴다.
-- 청크가 작을수록 압축/보존 정책이 더 촘촘하게 동작하고, 최근 구간 조회 시 스캔 대상도 줄어든다.
SELECT create_hypertable(
    'api_call_events',
    'occurred_at',
    chunk_time_interval => INTERVAL '1 day',
    if_not_exists => TRUE
);

-- create_hypertable이 (occurred_at DESC) 인덱스를 자동 생성하므로 여기서 다시 만들지 않는다.
CREATE INDEX IF NOT EXISTS idx_api_call_events_client_time
    ON api_call_events (api_client_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_api_call_events_path_pattern_time
    ON api_call_events (path_pattern, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_api_call_events_status_time
    ON api_call_events (status, occurred_at DESC);

-- =============================================================================
-- Continuous aggregate (계층형)
--
--   api_call_events (raw) -> _daily -> _monthly -> _yearly
--
-- monthly/yearly를 raw에서 다시 계산하지 않고 한 단계 아래 집계를 롤업한다.
-- raw를 읽는 것은 _daily 하나뿐이라 상위 집계의 refresh 비용이 크게 줄어든다.
--
-- 평균을 그대로 저장하지 않고 sum(duration_ms)를 저장하는 이유:
-- 상위 단계에서 avg(average_duration_ms)를 하면 "평균의 평균"이 되어 값이 틀어진다.
-- 합계와 건수를 들고 있으면 어느 단계에서든 정확한 가중평균을 복원할 수 있다.
-- =============================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS api_key_call_stats_daily
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', occurred_at) AS bucket,
    api_client_id,
    api_client_name,
    method,
    path_pattern,
    count(*) AS total_calls,
    count(*) FILTER (WHERE status >= 400) AS failed_calls,
    sum(duration_ms) AS total_duration_ms,
    max(duration_ms) AS max_duration_ms
FROM api_call_events
GROUP BY bucket, api_client_id, api_client_name, method, path_pattern;

-- 롤업 뷰는 GROUP BY를 반드시 위치 참조(1, 2, ...)로 쓴다.
-- 출력 별칭 bucket과 원본 컬럼 bucket이 이름이 같아서 "GROUP BY bucket"이라고 쓰면
-- Postgres가 원본 컬럼으로 해석하고, GROUP BY에 time_bucket()이 없다고 판단한
-- TimescaleDB가 "must include a valid time bucket function" 오류를 낸다.
CREATE MATERIALIZED VIEW IF NOT EXISTS api_key_call_stats_monthly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 month', bucket) AS bucket,
    api_client_id,
    api_client_name,
    method,
    path_pattern,
    sum(total_calls) AS total_calls,
    sum(failed_calls) AS failed_calls,
    sum(total_duration_ms) AS total_duration_ms,
    max(max_duration_ms) AS max_duration_ms
FROM api_key_call_stats_daily
GROUP BY 1, 2, 3, 4, 5;

CREATE MATERIALIZED VIEW IF NOT EXISTS api_key_call_stats_yearly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 year', bucket) AS bucket,
    api_client_id,
    api_client_name,
    method,
    path_pattern,
    sum(total_calls) AS total_calls,
    sum(failed_calls) AS failed_calls,
    sum(total_duration_ms) AS total_duration_ms,
    max(max_duration_ms) AS max_duration_ms
FROM api_key_call_stats_monthly
GROUP BY 1, 2, 3, 4, 5;

-- realtime aggregation.
-- TimescaleDB 2.13+ 는 신규 CAGG를 materialized_only = true 로 만든다.
-- 그 상태에서는 아직 materialize 되지 않은 최신 구간이 조회 결과에서 통째로 빠지기 때문에,
-- k6를 1분 돌리고 대시보드를 열면 CAGG 패널이 빈 화면이 된다.
-- false로 두면 "materialize된 과거 + 실시간 계산한 최근"을 합쳐서 돌려준다.
ALTER MATERIALIZED VIEW api_key_call_stats_daily   SET (timescaledb.materialized_only = false);
ALTER MATERIALIZED VIEW api_key_call_stats_monthly SET (timescaledb.materialized_only = false);
ALTER MATERIALIZED VIEW api_key_call_stats_yearly  SET (timescaledb.materialized_only = false);

-- refresh policy.
-- end_offset 이후 구간은 스케줄러가 materialize 하지 않는다(realtime aggregation이 대신 메꾼다).
-- 상위 집계일수록 갱신 주기를 길게 잡아 불필요한 재계산을 줄인다.
SELECT add_continuous_aggregate_policy('api_key_call_stats_daily',
    start_offset => INTERVAL '30 days',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute',
    if_not_exists => TRUE
);

SELECT add_continuous_aggregate_policy('api_key_call_stats_monthly',
    start_offset => INTERVAL '1 year',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '10 minutes',
    if_not_exists => TRUE
);

SELECT add_continuous_aggregate_policy('api_key_call_stats_yearly',
    start_offset => INTERVAL '5 years',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE
);

-- =============================================================================
-- 압축 / 보존 정책
--
-- 집계를 CAGG에 위임했기 때문에 raw 이벤트는 "최근 조회 + 재집계용"으로만 있으면 된다.
-- 오래된 청크는 압축하고, 더 오래된 청크는 아예 버려도 통계는 CAGG에 남는다.
-- 이것이 배치로 요약 테이블을 직접 관리할 때와 가장 크게 갈리는 지점이다.
-- =============================================================================

ALTER TABLE api_call_events SET (
    timescaledb.compress,
    -- 조회가 클라이언트/엔드포인트 기준으로 몰리므로 그 축으로 세그먼트를 나눈다.
    timescaledb.compress_segmentby = 'api_client_id, path_pattern',
    -- stream_id를 orderby에 포함해야 PK (stream_id, occurred_at) 를 압축 청크에서도 다룰 수 있다.
    timescaledb.compress_orderby = 'occurred_at DESC, stream_id'
);

SELECT add_compression_policy('api_call_events', INTERVAL '7 days', if_not_exists => TRUE);

-- 보존 기간은 daily CAGG의 start_offset(30일)보다 넉넉히 길게 잡는다.
-- refresh 창이 이미 삭제된 청크를 건드리지 않도록 하기 위함이다.
SELECT add_retention_policy('api_call_events', INTERVAL '180 days', if_not_exists => TRUE);
