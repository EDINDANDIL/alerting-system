package ru.kafka.listeners;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.models.dto.AlertEvent;
import ru.services.ConnectionManager;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.kafka.common.annotation.KafkaListener;

import java.util.Set;

@Component
public class AlertListener {

    private final ConnectionManager connectionManager;
    private static final Logger log = LoggerFactory.getLogger(AlertListener.class);

    public AlertListener(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @KafkaListener("kafka.alertConsumer")
    public void handle(String key, @Json AlertEvent alert) {

        Set<Integer> subscribers = alert.subscribers();

        if (subscribers == null || subscribers.isEmpty()) {
            log.warn("Empty subscribers list for alert");
            return;
        }

        log.debug("Received alert: key={}, subscribers={}, symbol={}",
                key, subscribers.size(), alert.symbol());

        connectionManager.sendToAll(subscribers, alert.asMessage()).whenComplete((v, ex) -> {
            if (ex != null) {
                log.warn("Что-то не так {}", ex.getMessage());
            } else {
                log.info("Обработали key={}, alert={}", key, alert);
            }
        });
    }
}
