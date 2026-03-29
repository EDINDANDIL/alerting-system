-- Миграция V1: Создание основной схемы БД для filter-service

-- Таблица фильтров IMPULSE
CREATE TABLE IF NOT EXISTS impulse_filters (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    exchanges TEXT[] NOT NULL DEFAULT '{}',
    markets TEXT[] NOT NULL DEFAULT '{}',
    blacklist TEXT[] NOT NULL DEFAULT '{}',
    time_window_sec INTEGER NOT NULL,
    direction_code VARCHAR(10) NOT NULL,
    percent INTEGER NOT NULL,
    volume_24h BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Таблица подписок пользователей на фильтры
CREATE TABLE IF NOT EXISTS user_impulse_filters (
    user_id INTEGER NOT NULL,
    impulse_id BIGINT NOT NULL REFERENCES impulse_filters(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, impulse_id)
);

-- Outbox таблица для гарантированной доставки событий
CREATE TABLE IF NOT EXISTS filter_outbox (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    filter_id BIGINT NOT NULL,
    user_id INTEGER,
    payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Индексы для производительности
CREATE INDEX IF NOT EXISTS idx_user_impulse_filters_user_id ON user_impulse_filters(user_id);
CREATE INDEX IF NOT EXISTS idx_user_impulse_filters_impulse_id ON user_impulse_filters(impulse_id);
CREATE INDEX IF NOT EXISTS idx_filter_outbox_created_at ON filter_outbox(created_at);
CREATE INDEX IF NOT EXISTS idx_filter_outbox_operation ON filter_outbox(operation);
CREATE INDEX IF NOT EXISTS idx_impulse_filters_action ON impulse_filters(action);

-- Триггер для обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_impulse_filters_updated_at
    BEFORE UPDATE ON impulse_filters
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
