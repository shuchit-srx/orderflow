CREATE TABLE outbox_events
(
    id               UUID PRIMARY KEY,

    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     UUID         NOT NULL,

    event_type       VARCHAR(100) NOT NULL,
    event_version    INTEGER      NOT NULL,

    exchange_name    VARCHAR(200) NOT NULL,
    routing_key      VARCHAR(200) NOT NULL,

    payload           TEXT         NOT NULL,

    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    attempts          INTEGER      NOT NULL DEFAULT 0,

    next_attempt_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    locked_at         TIMESTAMPTZ,

    published_at      TIMESTAMPTZ,

    last_error        TEXT,

    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_outbox_status
        CHECK (
            status IN (
                       'PENDING',
                       'PROCESSING',
                       'PUBLISHED',
                       'FAILED'
                )
            ),

    CONSTRAINT chk_outbox_attempts
        CHECK (attempts >= 0),

    CONSTRAINT uq_outbox_business_event
        UNIQUE (
                aggregate_type,
                aggregate_id,
                event_type,
                event_version
            )
);

CREATE INDEX idx_outbox_publishable
    ON outbox_events (
                      status,
                      next_attempt_at,
                      created_at
        );

CREATE INDEX idx_outbox_aggregate
    ON outbox_events (
                      aggregate_type,
                      aggregate_id
        );