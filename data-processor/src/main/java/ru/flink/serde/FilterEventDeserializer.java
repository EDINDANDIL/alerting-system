package ru.flink.serde;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.mappers.serde.OutboxCreatedEventDeserializer;

import java.io.IOException;

public final class FilterEventDeserializer
        implements DeserializationSchema<OutboxCreatedEvent> {

    private final OutboxCreatedEventDeserializer deserializer = new OutboxCreatedEventDeserializer();

    @Override
    public OutboxCreatedEvent deserialize(byte[] message) throws IOException {
        try {
            return deserializer.deserialize("filter-topic", message);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize OutboxCreatedEvent", e);
        }
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