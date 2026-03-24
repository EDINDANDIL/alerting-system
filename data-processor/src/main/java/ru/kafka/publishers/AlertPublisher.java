package ru.kafka.publishers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.models.dto.AlertEvent;
import ru.tinkoff.kora.common.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertPublisher.class);

    public CompletionStage<Void> sendAsync(String key, AlertEvent event) {
        return CompletableFuture.runAsync(() -> {
            log.info("Sent: key:{}, value:{}", key, event);
        });
    }

}
