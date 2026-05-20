package ru.services.kafka.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.common.dto.AlertCreatedEvent;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.kafka.common.annotation.KafkaListener;

@Component
public final class Listener {

    private static final Logger log = LoggerFactory.getLogger(Listener.class);

    // TODO написать конфиг для кафкить
    @KafkaListener("kafka.alertConsumer")
    public void handle(String key, AlertCreatedEvent event) {
        log.info("received {}, {}",key, event);
    }
}
