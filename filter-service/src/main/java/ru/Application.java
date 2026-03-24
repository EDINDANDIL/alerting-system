package ru;

import ru.common.mappers.direction.DirectionJsonReader;
import ru.common.mappers.direction.DirectionJsonWriter;
import ru.common.mappers.jdbc.JdbcJsonbMapperModule;
import ru.common.mappers.outbox.EventOutboxMapper;
import ru.common.mappers.outbox.OutboxMapperFacade;
import ru.common.util.Direction;
import ru.tinkoff.kora.application.graph.KoraApplication;
import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.config.hocon.HoconConfigModule;
import ru.tinkoff.kora.database.jdbc.JdbcDatabaseModule;
import ru.tinkoff.kora.http.server.undertow.UndertowHttpServerModule;
import ru.tinkoff.kora.json.common.JsonReader;
import ru.tinkoff.kora.json.common.JsonWriter;
import ru.tinkoff.kora.json.module.JsonModule;
import ru.tinkoff.kora.logging.logback.LogbackModule;

@KoraApp
public interface Application extends
        HoconConfigModule,
        UndertowHttpServerModule,
        JdbcDatabaseModule,
        JsonModule,
        LogbackModule,
        JdbcJsonbMapperModule {

    default EventOutboxMapper eventOutboxMapper() {
        return new ru.common.mappers.outbox.EventOutboxMapperImpl();
    }

    default OutboxMapperFacade outboxMapperFacade(EventOutboxMapper mapper) {
        return new OutboxMapperFacade(mapper);
    }

    default JsonReader<Direction> directionJsonReader() {
        return new DirectionJsonReader();
    }

    default JsonWriter<Direction> directionJsonWriter() {
        return new DirectionJsonWriter();
    }

    static void main(String[] args) {
        KoraApplication.run(ApplicationGraph::graph);
    }
}