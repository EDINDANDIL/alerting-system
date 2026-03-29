package ru.config;

import ru.tinkoff.kora.config.common.Config;
import ru.tinkoff.kora.config.common.extractor.ConfigValueExtractor;

/**
 * Конфигурация для Alert Service.
 * Читает настройки из application.conf через Kora Config.
 */
public record AlertConfig(
        Integer wsPort
) {
    public AlertConfig {
        // wsPort читается из application.conf: alert.ws.port
    }

    public static AlertConfig create(Config config, ConfigValueExtractor<Integer> intExtractor) {
        Integer wsPort = intExtractor.extract(config.get("alert.ws.port"));
        return new AlertConfig(wsPort);
    }
}
