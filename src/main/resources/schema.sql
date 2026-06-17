CREATE TABLE IF NOT EXISTS event_logs (
    event_id text PRIMARY KEY,
    event_type text NOT NULL,
    source text NOT NULL,
    schema_version text NOT NULL,
    occurred_at timestamptz NOT NULL,
    user_id text NOT NULL,
    anonymous_id text NOT NULL,
    session_id text NOT NULL,
    device_type text NOT NULL,
    traffic_source text NOT NULL,
    membership_level text NOT NULL,
    lifetime_purchase_count integer NOT NULL,
    lifetime_purchase_amount numeric(12, 2) NOT NULL,
    ab_test_group text NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_event_logs_event_type ON event_logs (event_type);
CREATE INDEX IF NOT EXISTS idx_event_logs_occurred_at ON event_logs (occurred_at);
CREATE INDEX IF NOT EXISTS idx_event_logs_user_id ON event_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_event_logs_session_id ON event_logs (session_id);

CREATE TABLE IF NOT EXISTS view_event_details (
    event_id text PRIMARY KEY REFERENCES event_logs (event_id) ON DELETE CASCADE,
    page_name text NOT NULL,
    page_url text NOT NULL,
    referrer text,
    course_id text,
    creator_id text,
    course_price numeric(12, 2)
);

CREATE TABLE IF NOT EXISTS click_event_details (
    event_id text PRIMARY KEY REFERENCES event_logs (event_id) ON DELETE CASCADE,
    page_name text NOT NULL,
    page_url text NOT NULL,
    component_name text NOT NULL,
    component_type text NOT NULL,
    course_id text,
    creator_id text,
    payment_method text,
    amount numeric(12, 2),
    currency text
);

CREATE TABLE IF NOT EXISTS request_event_details (
    event_id text PRIMARY KEY REFERENCES event_logs (event_id) ON DELETE CASCADE,
    api_method text NOT NULL,
    api_path text NOT NULL,
    request_name text NOT NULL,
    http_status integer NOT NULL,
    request_id text NOT NULL,
    purchase_id text,
    course_id text,
    creator_id text,
    amount numeric(12, 2),
    currency text,
    payment_method text,
    failure_reason text
);

CREATE TABLE IF NOT EXISTS auth_event_details (
    event_id text PRIMARY KEY REFERENCES event_logs (event_id) ON DELETE CASCADE,
    auth_provider text NOT NULL,
    page_url text NOT NULL
);
