package ru.kafka.listeners;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.models.dto.TradeEvent;
import ru.services.TradeProcessor;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.kafka.common.annotation.KafkaListener;
import ru.tinkoff.kora.logging.common.annotation.Log;

@Component
public class TradesListener {

    private final TradeProcessor tradesProcessor;
    private static final Logger log = LoggerFactory.getLogger(TradesListener.class);

    public TradesListener(TradeProcessor tradesProcessor) {
        this.tradesProcessor = tradesProcessor;
    }
    @KafkaListener("kafka.TradesConsumer")
    void handle(String key, @Json TradeEvent event, @Log.off Exception exception) {
        if (exception != null) return;
        log.debug("Приняли событие {}", key);
        tradesProcessor.onTrade(key, event);
    }
}