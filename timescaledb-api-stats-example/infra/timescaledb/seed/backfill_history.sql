-- 과거 시점 API 호출 이벤트를 대량 생성한다.
--
-- 왜 필요한가:
--   k6는 "지금" 트래픽만 만든다. 그래서 압축 정책(7일 경과 청크), 보존 정책,
--   월/년 단위 continuous aggregate, raw와 aggregate의 조회 성능 차이가 전부 드러나지 않는다.
--   이 스크립트로 과거 구간을 채우면 그 차이를 눈으로 볼 수 있다.
--
-- 사용법:
--   docker compose exec -T timescaledb psql -U api_stats -d api_stats \
--     -v days=400 -v events_per_day=5000 < infra/timescaledb/seed/backfill_history.sql
--
--   기본값은 400일 x 2000건 = 80만 건이다. 생성에 수십 초가 걸릴 수 있다.
--   400일로 잡으면 연 단위 집계가 두 해에 걸쳐 비교된다.

\if :{?days}
\else
\set days 400
\endif

\if :{?events_per_day}
\else
\set events_per_day 2000
\endif

\set ON_ERROR_STOP on

\echo '생성 대상:' :days '일 x' :events_per_day '건/일'

-- 정책 job이 중간에 끼어들지 않도록 잠시 멈춘다(생성 후 다시 켠다).
SELECT alter_job(job_id, scheduled => false)
FROM timescaledb_information.jobs
WHERE proc_name IN ('policy_compression', 'policy_retention', 'policy_refresh_continuous_aggregate');

INSERT INTO api_call_events (
    stream_id, occurred_at, api_client_id, api_client_name, auth_result,
    denied_reason, method, path, path_pattern, status, duration_ms,
    client_ip, user_agent, error_type
)
SELECT
    format('seed-%s-%s', x.day_offset, x.seq) AS stream_id,
    -- 하루 안에서 고르게 흩뿌리되, 업무 시간대(9~18시)에 가중치를 준다.
    (date_trunc('day', now()) - make_interval(days => x.day_offset))
        + make_interval(secs => (CASE WHEN x.rnd_hour < 0.7 THEN 32400 + x.rnd_sec * 32400 ELSE x.rnd_sec * 86400 END)::int) AS occurred_at,
    c.ids[x.client_idx] AS api_client_id,
    c.names[x.client_idx] AS api_client_name,
    'ALLOWED' AS auth_result,
    NULL AS denied_reason,
    r.methods[x.route_idx] AS method,
    r.patterns[x.route_idx] AS path,
    r.patterns[x.route_idx] AS path_pattern,
    -- 약 4% 실패(4xx/5xx), 나머지는 성공.
    CASE
        WHEN x.rnd_status < 0.02 THEN 500
        WHEN x.rnd_status < 0.04 THEN 403
        ELSE 200
    END AS status,
    -- 대부분 빠르고 가끔 느린 롱테일 분포.
    (CASE WHEN x.rnd_dur < 0.95 THEN 5 + x.rnd_dur2 * 60 ELSE 200 + x.rnd_dur2 * 1800 END)::bigint AS duration_ms,
    format('10.0.%s.%s', (x.client_idx * 7) % 256, (x.seq % 256)) AS client_ip,
    'k6-backfill/1.0' AS user_agent,
    CASE WHEN x.rnd_status < 0.02 THEN 'IllegalStateException' ELSE NULL END AS error_type
FROM (
    SELECT
        d AS day_offset,
        n AS seq,
        1 + floor(random() * (SELECT count(*) FROM api_clients))::int AS client_idx,
        1 + floor(random() * (SELECT count(*) FROM api_routes))::int AS route_idx,
        random() AS rnd_hour,
        random() AS rnd_sec,
        random() AS rnd_status,
        random() AS rnd_dur,
        random() AS rnd_dur2
    FROM generate_series(1, :days) AS d
    CROSS JOIN generate_series(1, :events_per_day) AS n
) x
CROSS JOIN (
    SELECT array_agg(id ORDER BY id) AS ids, array_agg(name ORDER BY id) AS names FROM api_clients
) c
CROSS JOIN (
    SELECT array_agg(method ORDER BY id) AS methods, array_agg(path_pattern ORDER BY id) AS patterns FROM api_routes
) r
ON CONFLICT (stream_id, occurred_at) DO NOTHING;

\echo '이벤트 생성 완료. continuous aggregate를 materialize 합니다...'

-- 계층 순서대로 채운다. 상위 집계는 하위 집계의 "materialize된" 데이터를 읽으므로 순서가 중요하다.
--
-- 끝 경계를 이번 달 시작으로 잡는 이유(NULL로 두면 안 되는 이유):
--   refresh 범위를 NULL(=끝까지)로 주면 현재 월/년 버킷까지 materialize 되면서
--   watermark가 그 버킷 너머로 올라간다. 그러면 realtime aggregation이 메꿔줄 구간이 없어져,
--   이후 새로 들어오는 이벤트가 다음 정책 refresh 전까지 월/년 집계에 안 잡힌다.
--   현재 버킷을 일부러 비워두면 realtime aggregation이 계속 최신 상태를 채워준다.
CALL refresh_continuous_aggregate('api_key_call_stats_daily', NULL, date_trunc('month', now()));
CALL refresh_continuous_aggregate('api_key_call_stats_monthly', NULL, date_trunc('month', now()));
CALL refresh_continuous_aggregate('api_key_call_stats_yearly', NULL, date_trunc('month', now()));

\echo '오래된 청크를 압축합니다...'

SELECT compress_chunk(c, if_not_compressed => true)
FROM show_chunks('api_call_events', older_than => INTERVAL '7 days') c;

-- 멈춰둔 정책 job을 다시 켠다.
SELECT alter_job(job_id, scheduled => true)
FROM timescaledb_information.jobs
WHERE proc_name IN ('policy_compression', 'policy_retention', 'policy_refresh_continuous_aggregate');

\echo ''
\echo '=== 결과 ==='

SELECT
    (SELECT count(*) FROM api_call_events) AS raw_events,
    (SELECT count(*) FROM api_key_call_stats_daily) AS daily_rows,
    (SELECT count(*) FROM api_key_call_stats_monthly) AS monthly_rows,
    (SELECT count(*) FROM api_key_call_stats_yearly) AS yearly_rows;

SELECT
    count(*) AS total_chunks,
    count(*) FILTER (WHERE is_compressed) AS compressed_chunks
FROM timescaledb_information.chunks
WHERE hypertable_name = 'api_call_events';

SELECT
    pg_size_pretty(sum(before_compression_total_bytes)) AS before_compression,
    pg_size_pretty(sum(after_compression_total_bytes)) AS after_compression,
    round(
        100.0 * (1 - sum(after_compression_total_bytes)::numeric / nullif(sum(before_compression_total_bytes), 0)),
        1
    ) AS saved_percent
FROM hypertable_compression_stats('api_call_events');
