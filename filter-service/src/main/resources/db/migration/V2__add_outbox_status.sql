-- Миграция V2: Добавление статуса и механизма повторных попыток в filter_outbox

ALTER TABLE filter_outbox
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_error TEXT,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_filter_outbox_status ON filter_outbox(status);

CREATE TRIGGER update_filter_outbox_updated_at
    BEFORE UPDATE ON filter_outbox
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
