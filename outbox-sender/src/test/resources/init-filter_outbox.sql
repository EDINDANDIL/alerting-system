CREATE TABLE IF NOT EXISTS filter_outbox (
                                             id           BIGSERIAL PRIMARY KEY,
                                             action       TEXT        NOT NULL,
                                             operation    SMALLINT    NOT NULL,
                                             filter_id    BIGINT      NOT NULL,
                                             user_id      INTEGER     NOT NULL,
                                             payload      JSONB,
                                             created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);