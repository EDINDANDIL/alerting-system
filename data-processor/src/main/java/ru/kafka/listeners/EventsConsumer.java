package ru.kafka.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.common.dto.OutboxCreatedEvent;
import ru.core.FilterEventHandlerRegistry;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.tinkoff.kora.kafka.common.annotation.KafkaListener;
import ru.tinkoff.kora.logging.common.annotation.Log;

@Component
public final class EventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventsConsumer.class);

    private final FilterEventHandlerRegistry registry;

    public EventsConsumer(FilterEventHandlerRegistry registry) {
        this.registry = registry;
    }

    @KafkaListener("kafka.eventConsume")
    void handle(String key, @Json OutboxCreatedEvent event, @Log.off Exception exception) {
        if (exception != null) {
            log.warn("Ошибка {}", exception.getMessage());
            return;
        }

        log.debug(
                "FILTER_EVENT key={} action={} op={} filterId={} userId={} createdAt={} payloadType={}",
                key,
                event.action(),
                event.operation(),
                event.filterId(),
                event.userId(),
                event.createdAt(),
                event.payload() == null ? "null" : event.payload().getClass().getSimpleName()
        );

        try {

            registry.get(event.action()).apply(event);

            log.debug("FILTER_EVENT applied: action={} op={} filterId={}", event.action(), event.operation(), event.filterId());
        } catch (RuntimeException e) {
            log.error("FILTER_EVENT apply failed: action={} op={} filterId={}", event.action(), event.operation(), event.filterId(), e);
            throw e;
        }
    }
}