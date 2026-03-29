package ru.kafka.listeners;

import ru.models.dto.TradeEvent;
import ru.services.trades.TradesProcessor;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.kafka.common.annotation.KafkaListener;
import ru.tinkoff.kora.logging.common.annotation.Log;

@Component
public class TradesListener {

    private final TradesProcessor tradesProcessor;

    public TradesListener(TradesProcessor tradesProcessor) {
        this.tradesProcessor = tradesProcessor;
    }
    @KafkaListener("kafka.TradesConsumer")
    void handle(String key, @Json TradeEvent event, @Log.off Exception exception) {
        if (exception != null) return;
        tradesProcessor.process(key, event);
    }
}