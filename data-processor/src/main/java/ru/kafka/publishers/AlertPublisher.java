package ru.kafka.publishers;


import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import ru.models.dto.AlertEvent;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.kafka.common.annotation.KafkaPublisher;

import java.util.concurrent.CompletableFuture;

@KafkaPublisher("kafka.alertProducer")
public interface AlertPublisher {
    CompletableFuture<RecordMetadata> send(ProducerRecord<String, @Json AlertEvent> record);
}
