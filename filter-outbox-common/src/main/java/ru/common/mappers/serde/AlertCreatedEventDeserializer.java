package ru.common.mappers.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.common.dto.AlertCreatedEvent;

public final class AlertCreatedEventDeserializer implements Deserializer<AlertCreatedEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public AlertCreatedEvent deserialize(String topic, byte[] bytes) {
        try {
            if (bytes == null) return null;
            return MAPPER.readValue(bytes, AlertCreatedEvent.class);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}
