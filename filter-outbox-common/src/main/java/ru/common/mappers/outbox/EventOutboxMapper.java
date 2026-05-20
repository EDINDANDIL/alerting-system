package ru.common.mappers.outbox;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.common.dto.FilterCreatedEvent;
import ru.common.persistence.entity.FilterOutboxEntity;

@Mapper
public interface EventOutboxMapper {

    @Mapping(target = "eventId", ignore = true)
    FilterOutboxEntity asEntity(FilterCreatedEvent event);
    FilterCreatedEvent asEvent(FilterOutboxEntity entity);
}