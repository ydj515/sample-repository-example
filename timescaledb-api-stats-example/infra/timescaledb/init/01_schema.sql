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
    auth_result varchar(30) NOT NULL,
    denied_reason text,
    method varchar(10) NOT NULL,
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

SELECT create_hypertable('api_call_events', 'occurred_at', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_api_call_events_time_desc
    ON api_call_events (occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_api_call_events_client_time
    ON api_call_events (api_client_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_api_call_events_path_pattern_time
    ON api_call_events (path_pattern, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_api_call_events_status_time
    ON api_call_events (status, occurred_at DESC);
