package ru.common.mappers.serde;

import ru.common.dto.*;
import ru.common.mappers.direction.DirectionJsonReader;
import ru.common.mappers.direction.DirectionJsonWriter;
import ru.common.util.$OutboxOperation_JsonReader;
import ru.common.util.$OutboxOperation_JsonWriter;
import ru.tinkoff.kora.json.common.JsonReader;
import ru.tinkoff.kora.json.common.JsonWriter;
import ru.tinkoff.kora.json.module.JsonModule;

public final class OutboxJsonFactory {
    private OutboxJsonFactory() {}

    public static JsonReader<OutboxCreatedEvent> getReader() {
        JsonModule json = new JsonModule() {};
        var stringReader = json.stringJsonReader();
        var operationReader = new $OutboxOperation_JsonReader(stringReader);
        var offsetDateTimeReader = json.offsetDateTimeJsonReader();
        var stringSetReader = json.setJsonReaderFactory(stringReader);
        var directionReader = new DirectionJsonReader();
        var impulseReader = new $OutboxPayload_ImpulseFilter_JsonReader(
                stringSetReader,
                stringSetReader,
                stringSetReader,
                directionReader
        );
        var payloadReader = new $OutboxPayload_JsonReader(impulseReader);
        return new $OutboxCreatedEvent_JsonReader(
                operationReader,
                offsetDateTimeReader,
                payloadReader
        );
    }

    public static JsonWriter<OutboxCreatedEvent> getWriter() {
        JsonModule json = new JsonModule() {};
        var stringWriter = json.stringJsonWriter();
        var operationWriter = new $OutboxOperation_JsonWriter(stringWriter);
        var offsetDateTimeWriter = json.offsetDateTimeJsonWriter();
        var stringSetWriter = json.setJsonWriterFactory(stringWriter);
        var directionWriter = new DirectionJsonWriter();
        var impulseWriter = new $OutboxPayload_ImpulseFilter_JsonWriter(
                stringSetWriter,
                stringSetWriter,
                stringSetWriter,
                directionWriter
        );
        var payloadWriter = new $OutboxPayload_JsonWriter(impulseWriter);
        return new $OutboxCreatedEvent_JsonWriter(
                operationWriter,
                offsetDateTimeWriter,
                payloadWriter
        );
    }
}
