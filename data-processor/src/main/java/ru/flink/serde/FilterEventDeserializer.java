package ru.flink.serde;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import ru.common.dto.$OutboxCreatedEvent_JsonReader;
import ru.common.dto.$OutboxPayload_ImpulseFilter_JsonReader;
import ru.common.dto.$OutboxPayload_JsonReader;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.mappers.direction.DirectionJsonReader;
import ru.common.util.$OutboxOperation_JsonReader;
import ru.tinkoff.kora.json.common.JsonReader;
import ru.tinkoff.kora.json.module.JsonModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class FilterEventDeserializer
        implements DeserializationSchema<OutboxCreatedEvent> {

    private transient JsonReader<OutboxCreatedEvent> reader;

    @Override
    public void open(InitializationContext context) {
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

        reader = new $OutboxCreatedEvent_JsonReader(
                operationReader,
                offsetDateTimeReader,
                payloadReader
        );
    }

    @Override
    public OutboxCreatedEvent deserialize(byte[] message) throws IOException {
        return reader.read(new String(message, StandardCharsets.UTF_8));
    }

    @Override
    public boolean isEndOfStream(OutboxCreatedEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<OutboxCreatedEvent> getProducedType() {
        return TypeInformation.of(OutboxCreatedEvent.class);
    }
}