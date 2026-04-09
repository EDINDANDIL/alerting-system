package ru;

import ru.config.GeneratorConfig;
import ru.tinkoff.kora.application.graph.KoraApplication;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.config.common.Config;
import ru.tinkoff.kora.config.common.extractor.ConfigValueExtractor;
import ru.tinkoff.kora.config.hocon.HoconConfigModule;
import ru.tinkoff.kora.http.server.undertow.UndertowHttpServerModule;
import ru.tinkoff.kora.json.module.JsonModule;
import ru.tinkoff.kora.kafka.common.KafkaModule;
import ru.tinkoff.kora.kafka.common.KafkaSerializersModule;

@KoraApp
public interface Application extends
        HoconConfigModule,
        UndertowHttpServerModule,
        KafkaModule,
        JsonModule,
        KafkaSerializersModule {

    default GeneratorConfig generatorConfig(
            Config config,
            ConfigValueExtractor<Long> longExtractor,
            ConfigValueExtractor<Double> doubleExtractor,
            ConfigValueExtractor<Integer> intExtractor
    ) {
        return GeneratorConfig.create(config, longExtractor, doubleExtractor, intExtractor);
    }

    static void main(String[] args) {
        KoraApplication.run(ApplicationGraph::graph);
    }
}
