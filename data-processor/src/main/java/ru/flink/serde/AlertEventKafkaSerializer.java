package ru.flink.serde;

import org.apache.flink.api.common.serialization.SerializationSchema.InitializationContext;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import ru.common.dto.AlertCreatedEvent;
import ru.common.mappers.serde.AlertCreatedEventSerializer;

import java.nio.charset.StandardCharsets;

public final class AlertEventKafkaSerializer
        implements KafkaRecordSerializationSchema<AlertCreatedEvent> {

    private final String topic;
    private transient AlertCreatedEventSerializer serializer;

    public AlertEventKafkaSerializer(String topic) {this.topic = topic;}

    @Override
    public void open(InitializationContext context, KafkaSinkContext sinkContext) {
        serializer = new AlertCreatedEventSerializer();
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            AlertCreatedEvent event,
            KafkaSinkContext context,
            Long timestamp
    ) {
        byte[] key = event.symbol().getBytes(StandardCharsets.UTF_8);
        byte[] value = serializer().serialize(topic, event);

        return new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key,
                value
        );
    }

    private AlertCreatedEventSerializer serializer() {
        if (serializer == null) {
            serializer = new AlertCreatedEventSerializer();
        }
        return serializer;
    }
}
