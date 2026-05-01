package ru.flink.serde;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.mappers.serde.OutboxJsonFactory;
import ru.tinkoff.kora.json.common.JsonReader;

import java.io.IOException;

public final class FilterEventDeserializer
        implements DeserializationSchema<OutboxCreatedEvent> {

    private transient JsonReader<OutboxCreatedEvent> reader;

    @Override
    public void open(InitializationContext context) {
        this.reader = OutboxJsonFactory.getReader();
    }

    @Override
    public OutboxCreatedEvent deserialize(byte[] message) throws IOException {
        if (this.reader == null) {
            throw new IOException("OutboxCreatedEvent JsonReader is not initialized");
        }
        try {
            return this.reader.read(message);
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