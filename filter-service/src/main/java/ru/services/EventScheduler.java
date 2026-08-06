package ru.services;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.common.dto.FilterCreatedEvent;
import ru.common.mappers.outbox.OutboxMapperFacade;
import ru.common.persistence.entity.FilterOutboxEntity;
import ru.persistence.repository.OutboxRepository;
import ru.kafka.publishers.Publisher;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.scheduling.jdk.annotation.ScheduleAtFixedRate;

import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class EventScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventScheduler.class);
    private final OutboxRepository repository;
    private final Publisher publisher;
    private final OutboxMapperFacade facade;

    public EventScheduler(OutboxRepository repository, Publisher publisher, OutboxMapperFacade facade) {
        this.repository = repository;
        this.publisher = publisher;
        this.facade = facade;
    }

    @ScheduleAtFixedRate(initialDelay = 50, period = 1000, unit = ChronoUnit.MILLIS)
    public void send() {
        List<FilterOutboxEntity> entities = repository
                .getJdbcConnectionFactory().inTx(() -> repository.findNextBatchToProcess(10));

        if (entities == null || entities.isEmpty()) {
            return;
        }

        for (FilterOutboxEntity entity : entities) {
            long id = entity.eventId();
            try {
                FilterCreatedEvent event = facade.asEvent(entity);
                String key = entity.action() + ":" + entity.filterId();
                ProducerRecord<String, FilterCreatedEvent> record = new ProducerRecord<>(
                        "filter-topic",
                        key,
                        event
                );
                record.headers().add("event-id", Long.toString(id).getBytes(StandardCharsets.UTF_8));
                publisher.send(record);

                repository.getJdbcConnectionFactory().inTx(() -> repository.deleteById(id));
            } catch (Throwable t) {
                log.error("Failed to send outbox record with id: {}", id, t);
                String errorMessage = t.getMessage() != null ? t.getMessage() : t.toString();
                repository.getJdbcConnectionFactory().inTx(() -> repository.updateStatus(id, "FAILED", errorMessage));
            }
        }
    }
}