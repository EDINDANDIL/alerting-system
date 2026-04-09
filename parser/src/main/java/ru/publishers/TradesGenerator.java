package ru.publishers;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import ru.dto.TradeEvent;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.kafka.common.annotation.KafkaPublisher;

import java.util.concurrent.CompletableFuture;

@KafkaPublisher("kafka.TradesProducer")
public interface TradesGenerator {
    CompletableFuture<RecordMetadata> send(ProducerRecord<String, @Json TradeEvent> record);
}
