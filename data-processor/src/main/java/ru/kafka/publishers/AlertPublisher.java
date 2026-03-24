package ru.kafka.publishers;

import ru.models.dto.AlertEvent;
import ru.tinkoff.kora.common.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class AlertPublisher {

    public CompletionStage<Void> sendAsync(String key, AlertEvent event) {
        return CompletableFuture.runAsync(() -> {
            IO.println("ALERT key=" + key + " msg=" + event.message());
        });
    }

}
