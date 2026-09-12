CREATE TABLE processed_events
(
    event_id      UUID PRIMARY KEY,
    event_type    VARCHAR(100) NOT NULL,
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_events_processed_at
    ON processed_events (processed_at);

CREATE INDEX idx_processed_events_event_type
    ON processed_events (event_type);