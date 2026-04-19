package ru.publishers;

import org.apache.kafka.clients.producer.ProducerRecord;
import ru.dto.TradeEvent;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.kafka.common.annotation.KafkaPublisher;

@KafkaPublisher("kafka.TradesProducer")
public interface TradesGenerator {
    void send(ProducerRecord<String, @Json TradeEvent> record);
}
