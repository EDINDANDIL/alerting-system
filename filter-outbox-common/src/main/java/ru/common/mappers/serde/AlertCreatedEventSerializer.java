package ru.common.mappers.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import ru.common.dto.AlertCreatedEvent;

public final class AlertCreatedEventSerializer implements Serializer<AlertCreatedEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public byte[] serialize(String topic, AlertCreatedEvent event) {
        try {
            return MAPPER.writeValueAsBytes(event);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}
