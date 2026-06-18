CREATE TABLE IF NOT EXISTS event_logs (
    event_id text PRIMARY KEY,
    event_type text NOT NULL,
    source text NOT NULL,
    schema_version text NOT NULL,
    occurred_at timestamptz NOT NULL,
    user_id text NOT NULL,
    anonymous_id text NOT NULL,
    session_id text NOT NULL,
    device_type text NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_event_logs_event_type ON event_logs (event_type);
CREATE INDEX IF NOT EXISTS idx_event_logs_occurred_at ON event_logs (occurred_at);
CREATE INDEX IF NOT EXISTS idx_event_logs_user_id ON event_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_event_logs_session_id ON event_logs (session_id);

CREATE TABLE IF NOT EXISTS lecture_event_details (
    event_id text PRIMARY KEY REFERENCES event_logs (event_id) ON DELETE CASCADE,
    course_id text NOT NULL,
    lecture_id text NOT NULL,
    lecture_title text NOT NULL,
    playback_position_seconds integer NOT NULL,
    watch_duration_seconds integer NOT NULL,
    completion_rate numeric(5, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS video_error_event_details (
    event_id text PRIMARY KEY REFERENCES event_logs (event_id) ON DELETE CASCADE,
    course_id text NOT NULL,
    lecture_id text NOT NULL,
    error_type text NOT NULL,
    error_code text NOT NULL,
    error_message text NOT NULL
);
