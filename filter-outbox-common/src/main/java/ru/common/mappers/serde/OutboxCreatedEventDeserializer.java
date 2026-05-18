package ru.common.mappers.serde;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.common.dto.OutboxCreatedEvent;

public class OutboxCreatedEventDeserializer implements Deserializer<OutboxCreatedEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public OutboxCreatedEvent deserialize(String topic, byte[] bytes) {
        try {
            if (bytes == null) return null;
            return MAPPER.readValue(bytes, OutboxCreatedEvent.class);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}
