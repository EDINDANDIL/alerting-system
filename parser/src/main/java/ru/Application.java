package ru;

import ru.config.SimulatorConfig;
import ru.tinkoff.kora.application.graph.KoraApplication;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.config.common.Config;
import ru.tinkoff.kora.config.common.extractor.ConfigValueExtractor;
import ru.tinkoff.kora.config.hocon.HoconConfigModule;
import ru.tinkoff.kora.http.server.undertow.UndertowHttpServerModule;
import ru.tinkoff.kora.json.module.JsonModule;
import ru.tinkoff.kora.kafka.common.KafkaModule;
import ru.tinkoff.kora.logging.logback.LogbackModule;
import ru.tinkoff.kora.micrometer.module.MetricsModule;

@KoraApp
public interface Application extends
        HoconConfigModule,
        UndertowHttpServerModule,
        KafkaModule,
        LogbackModule,
        JsonModule,
        MetricsModule {

    static void main(String[] args) {
        KoraApplication.run(ApplicationGraph::graph);
    }

    default SimulatorConfig simulatorConfig(Config config, ConfigValueExtractor<SimulatorConfig> extractor) {
        return extractor.extract(config.get("simulator"));
    }
}