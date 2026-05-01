package ru.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import ru.dto.Message;
import ru.dto.TradeTick;
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
        String symbol = msg.symbol() == null || msg.symbol().isBlank() ? "BST" : msg.symbol();
        double percent = Double.parseDouble(msg.changePercent());
        boolean up = "UP".equals(msg.trend());

        for (int i = 0; i < count; i++) {
            if (i > 0 && i % steps == 0) {
                if (up) price = price * (100 + (long) percent) / 100;
                else price = price * (100 - (long) percent) / 100;
            }

            TradeTick event = new TradeTick(price, System.nanoTime());

            publisher.send(new ProducerRecord<>(
                    "trades-topic",
                    symbol,
                    TradeEventBinaryEncoder.encode(event)
            ));
        }
    }
}
