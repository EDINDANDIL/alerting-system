package ru.flink.serde;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import ru.common.dto.FilterCreatedEvent;
import ru.common.mappers.serde.FilterCreatedEventDeserializer;

import java.io.IOException;

public final class FilterEventDeserializer
        implements DeserializationSchema<FilterCreatedEvent> {

    private transient FilterCreatedEventDeserializer deserializer;

    @Override
    public FilterCreatedEvent deserialize(byte[] message) throws IOException {
        try {
            return deserializer().deserialize("filter-topic", message);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize OutboxCreatedEvent", e);
        }
    }

    private FilterCreatedEventDeserializer deserializer() {
        return deserializer == null ? new FilterCreatedEventDeserializer() : deserializer;
    }

    @Override
    public boolean isEndOfStream(FilterCreatedEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<FilterCreatedEvent> getProducedType() {
        return TypeInformation.of(FilterCreatedEvent.class);
    }
}
