package ru.publishers;

import org.apache.kafka.clients.producer.ProducerRecord;
import ru.tinkoff.kora.kafka.common.annotation.KafkaPublisher;

@KafkaPublisher("kafka.TradesProducer")
public interface TradesGenerator {
    void send(ProducerRecord<String, byte[]> record);
}
