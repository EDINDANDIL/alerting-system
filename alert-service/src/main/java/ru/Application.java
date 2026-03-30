package ru;

import ru.config.AlertConfig;
import ru.services.ConnectionManagerImpl;
import ru.services.WebSocketServer;
import ru.tinkoff.kora.application.graph.KoraApplication;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.config.common.Config;
import ru.tinkoff.kora.config.common.extractor.ConfigValueExtractor;
import ru.tinkoff.kora.config.hocon.HoconConfigModule;
import ru.tinkoff.kora.http.server.undertow.UndertowHttpServerModule;
import ru.tinkoff.kora.kafka.common.KafkaModule;
import ru.tinkoff.kora.json.module.JsonModule;
import ru.tinkoff.kora.logging.logback.LogbackModule;

@KoraApp
public interface Application extends
        HoconConfigModule,
        UndertowHttpServerModule,
        KafkaModule,
        JsonModule,
        LogbackModule
{
    static void main(String[] args) {
        KoraApplication.run(ApplicationGraph::graph);
    }

    default AlertConfig alertConfig(Config config, ConfigValueExtractor<Integer> intExtractor) {
        return AlertConfig.create(config, intExtractor);
    }
}
