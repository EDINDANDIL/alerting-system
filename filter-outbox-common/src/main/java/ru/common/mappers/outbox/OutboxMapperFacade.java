package ru.common.mappers.outbox;

import ru.common.dto.OutboxCreatedEvent;
import ru.common.persistence.entity.FilterOutboxEntity;
import ru.tinkoff.kora.common.Component;

@Component
public final class OutboxMapperFacade {
    private final EventOutboxMapper eventOutboxMapper;

    public OutboxMapperFacade(EventOutboxMapper eventOutboxMapper) {
        this.eventOutboxMapper = eventOutboxMapper;
    }

    public FilterOutboxEntity asEntity(OutboxCreatedEvent event) {
        return eventOutboxMapper.asEntity(event);
    }

    public OutboxCreatedEvent asEvent(FilterOutboxEntity entity) {
        return eventOutboxMapper.asEvent(entity);
    }
}