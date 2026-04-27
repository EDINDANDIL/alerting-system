package ru.flink.serde;

import org.apache.flink.api.common.serialization.SerializationSchema.InitializationContext;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import ru.flink.model.AlertEvent;
import ru.flink.model.$AlertEvent_JsonWriter;
import ru.tinkoff.kora.json.common.JsonWriter;
import ru.tinkoff.kora.json.module.JsonModule;

import java.nio.charset.StandardCharsets;

public final class AlertEventKafkaSerializer
        implements KafkaRecordSerializationSchema<AlertEvent> {

    private final String topic;
    private transient JsonWriter<AlertEvent> writer;

    public AlertEventKafkaSerializer(String topic) {this.topic = topic;}

    @Override
    public void open(InitializationContext context, KafkaSinkContext sinkContext) {
        JsonModule json = new JsonModule() {};

        var longWriter = json.longJsonWriter();
        var stringWriter = json.stringJsonWriter();

        var longSetWriter = json.setJsonWriterFactory(longWriter);
        var stringSetWriter = json.setJsonWriterFactory(stringWriter);

        writer = new $AlertEvent_JsonWriter(
                longSetWriter,
                stringSetWriter,
                stringSetWriter
        );
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(
            AlertEvent event,
            KafkaSinkContext context,
            Long timestamp
    ) {
        byte[] key = event.symbol().getBytes(StandardCharsets.UTF_8);
        byte[] value = writer.toStringUnchecked(event).getBytes(StandardCharsets.UTF_8);

        return new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key,
                value
        );
    }
}