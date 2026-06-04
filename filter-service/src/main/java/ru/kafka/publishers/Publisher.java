package ru.kafka.publishers;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import ru.common.dto.FilterCreatedEvent;
import ru.tinkoff.kora.kafka.common.annotation.KafkaPublisher;
import ru.tinkoff.kora.logging.common.annotation.Log;

import java.util.concurrent.CompletableFuture;

@KafkaPublisher("kafka.EventProducer")
public interface Publisher {

    @Log
    CompletableFuture<RecordMetadata> send(ProducerRecord<String, FilterCreatedEvent> record);
}
