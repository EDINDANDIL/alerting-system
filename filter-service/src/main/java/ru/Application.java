package ru;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import ru.common.dto.AlertCreatedEvent;
import ru.common.dto.FilterCreatedEvent;
import ru.common.mappers.jdbc.JdbcJsonbMapperModule;
import ru.common.mappers.serde.AlertCreatedEventDeserializer;
import ru.common.mappers.serde.FilterCreatedEventSerializer;
import ru.tinkoff.kora.application.graph.KoraApplication;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.config.hocon.HoconConfigModule;
import ru.tinkoff.kora.database.jdbc.JdbcDatabaseModule;
import ru.tinkoff.kora.http.server.undertow.UndertowHttpServerModule;
import ru.tinkoff.kora.json.module.JsonModule;
import ru.tinkoff.kora.kafka.common.KafkaModule;
import ru.tinkoff.kora.logging.logback.LogbackModule;
import ru.tinkoff.kora.scheduling.common.SchedulingModule;
import ru.tinkoff.kora.scheduling.jdk.SchedulingJdkModule;
import ru.tinkoff.kora.micrometer.module.MetricsModule;

@KoraApp
public interface Application extends
        HoconConfigModule,
        UndertowHttpServerModule,
        JdbcDatabaseModule,
        JsonModule,
        LogbackModule,
        JdbcJsonbMapperModule,
        SchedulingModule,
        SchedulingJdkModule,
        MetricsModule,
        KafkaModule {

    default Serializer<String> stringSerializer() {return new StringSerializer();}
    default Deserializer<String> stringDeserializer() {return new StringDeserializer();}
    default Serializer<FilterCreatedEvent> outboxCreatedEventSerializer() {return new FilterCreatedEventSerializer();}
    default Deserializer<AlertCreatedEvent> alertCreatedEventDeserializer() {return new AlertCreatedEventDeserializer();}

    static void main(String[] args) {
        KoraApplication.run(ApplicationGraph::graph);
    }
}