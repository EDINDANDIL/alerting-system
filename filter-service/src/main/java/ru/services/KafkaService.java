package ru.services;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.mappers.outbox.OutboxMapperFacade;
import ru.common.persistence.entity.FilterOutboxEntity;
import ru.persistence.repository.OutboxRepository;
import ru.services.kafka.publishers.Publisher;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.scheduling.jdk.annotation.ScheduleAtFixedRate;

import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaService {

    private static final Logger log = LoggerFactory.getLogger(KafkaService.class);
    private final OutboxRepository repository;
    private final Publisher publisher;
    private final OutboxMapperFacade facade;

    public KafkaService(OutboxRepository repository, Publisher publisher, OutboxMapperFacade facade) {
        this.repository = repository;
        this.publisher = publisher;
        this.facade = facade;
    }

    @ScheduleAtFixedRate(initialDelay = 50, period = 50, unit = ChronoUnit.MILLIS)
    public void send() {
        repository.getJdbcConnectionFactory().inTx(() -> {

            List<FilterOutboxEntity> entities = repository.findNextBatch(1);
            if (entities.isEmpty()) return null;

            for (FilterOutboxEntity entity : entities) {
                long id = entity.eventId();
                OutboxCreatedEvent event = facade.asEvent(entity);
                String key = entity.action() + ":" + entity.filterId();
                ProducerRecord<String, OutboxCreatedEvent> record = new ProducerRecord<>(
                        "filter-topic",
                        key,
                        event
                );
                log.info("event={}", event);
                record.headers().add("event-id", Long.toString(id).getBytes(StandardCharsets.UTF_8));
                try {
                    publisher.send(record).orTimeout(10, TimeUnit.SECONDS).join();
                } catch (Exception e) {
                    log.error("Failed to send outbox record with id: {}, transaction rollback", id, e);
                    throw new RuntimeException(e);
                }
            }

            entities.forEach(entity -> repository.deleteById(entity.eventId()));

            return null;
        });
    }
}
