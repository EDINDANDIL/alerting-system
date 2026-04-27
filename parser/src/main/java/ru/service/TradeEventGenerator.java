package ru.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import ru.dto.Message;
import ru.dto.TradeEvent;
import ru.publishers.TradesGenerator;
import ru.serde.TradeEventBinaryEncoder;
import ru.tinkoff.kora.common.Component;

@Component
public class TradeEventGenerator {

    private final TradesGenerator publisher;

    public TradeEventGenerator(TradesGenerator publisher) {
        this.publisher = publisher;
    }

    public void generate(Message msg) {
        long price = msg.startPrice();
        long steps = msg.steps();
        long count = msg.count();
        double percent = Double.parseDouble(msg.changePercent());
        boolean up = "UP".equals(msg.trend());

        for (int i = 0; i < count; i++) {
            if (i > 0 && i % steps == 0) {
                if (up) price = price * (100 + (long) percent) / 100;
                else price = price * (100 - (long) percent) / 100;
            }

            TradeEvent event = new TradeEvent(
                    "binance", "futures", "BST", 0L,
                    System.nanoTime(), 0L, 0L,
                    price, 0L, 0L, 0, 0
            );

            publisher.send(new ProducerRecord<>(
                    "trades-topic",
                    event.symbol(),
                    TradeEventBinaryEncoder.encode(event)
            ));
        }
    }
}
