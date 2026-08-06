package ru.kafka.publishers;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import ru.common.dto.FilterCreatedEvent;
import ru.tinkoff.kora.kafka.common.annotation.KafkaPublisher;
import ru.tinkoff.kora.logging.common.annotation.Log;

@KafkaPublisher("kafka.EventProducer")
public interface Publisher {

    @Log
    RecordMetadata send(ProducerRecord<String, FilterCreatedEvent> record);
}