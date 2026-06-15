package ru.publishers;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import ru.tinkoff.kora.kafka.common.annotation.KafkaPublisher;
import ru.tinkoff.kora.logging.common.annotation.Log;

import java.util.concurrent.CompletionStage;

@KafkaPublisher("kafka.TradesProducer")
public interface TradePublisher {
    @Log
    CompletionStage<RecordMetadata> send(ProducerRecord<String, byte[]> record);
}