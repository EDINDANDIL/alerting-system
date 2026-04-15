package ru.services;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.core.cache.Index;
import ru.kafka.listeners.TradesListener;
import ru.kafka.publishers.AlertPublisher;
import ru.models.dto.AlertEvent;
import ru.models.dto.TradeEvent;

import ru.tinkoff.kora.common.Component;

@Component
public final class TradeProcessor {

    private final Index index;
    private final AlertPublisher alertPublisher;
    private static final Logger log = LoggerFactory.getLogger(TradeProcessor.class);

    public TradeProcessor(Index index, AlertPublisher alertPublisher) {
        this.index = index;
        this.alertPublisher = alertPublisher;
    }

    public void onTrade(String key, TradeEvent event) {

        index.addPoint(event);

        index.check(event.symbol())
                .stream().filter(f -> !f.subscribers().isEmpty())
                .forEach(
    f -> {
            var alertCreatedEvent = new AlertEvent(
                    f.subscribers(),
                    f.payload().exchange(),
                    f.payload().market(),
                    event.symbol(),
                    event.timestampNs()
                    );
            alertPublisher.send(new ProducerRecord<>("alert-topic", key, alertCreatedEvent));
            log.info("Alert Sent {}", alertCreatedEvent);
        });
    }
}
