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
--
-- 보존 정책과의 관계:
--   raw 테이블에는 180일 보존 정책이 걸려 있다. days를 180보다 크게 주면 보존 job이
--   다음 실행 때 그만큼의 raw를 지운다. 집계 view는 그대로 남으므로
--   "원본은 버려도 통계는 남는다"를 보여 주는 구성이지만, 그 구간에서는
--   source=raw 와 source=aggregate 결과가 달라진다.
--   전 구간을 raw로도 비교하려면 days를 보존 기간보다 짧게 주면 된다.

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

-- 정책 job은 일부러 끄지 않는다.
--
-- 처음에는 alter_job(scheduled => false)로 멈췄다가 끝에서 되살리는 구조였는데,
-- ON_ERROR_STOP 상태에서 중간에 실패하면 job이 꺼진 채로 남는다. 압축/보존/집계 갱신이
-- 조용히 멈추고, 사용자는 그 사실을 알 방법이 없다. 복구를 보장할 수 없는 구조라 걷어냈다.
--
-- 끄지 않아도 안전한 이유:
--   - INSERT는 단일 문장이라 원자적이고, 정책이 중간 상태를 보지 않는다.
--   - 아래 압축은 if_not_compressed => true 라서 정책이 먼저 압축했어도 문제없다.
--   - refresh는 TimescaleDB가 뷰 단위로 직렬화한다.

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

\echo ''
\echo '=== 결과 ==='

-- 생성 구간이 보존 기간을 넘는지 알려 준다. 넘으면 보존 job이 그만큼의 raw를 지운다.
SELECT
    :days AS seeded_days,
    (config ->> 'drop_after') AS retention,
    CASE
        WHEN make_interval(days => :days) > (config ->> 'drop_after')::interval
        THEN '주의: 생성 구간이 보존 기간보다 깁니다. 보존 job 실행 후 오래된 raw는 삭제되고 집계만 남습니다.'
        ELSE '생성 구간이 보존 기간 안에 있습니다.'
    END AS note
FROM timescaledb_information.jobs
WHERE proc_name = 'policy_retention' AND hypertable_name = 'api_call_events';

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
