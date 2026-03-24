package ru.models.mappers.impulse;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.common.dto.OutboxPayload;
import ru.models.dto.Request;
import ru.persistence.entity.ImpulseFilterEntity;


@Mapper
public interface ImpulseFilterMapper {

    @Mapping(target = "id", ignore = true)
    ImpulseFilterEntity toEntity(Request.ImpulseFilterDto dto);

    Request.ImpulseFilterDto toDto(ImpulseFilterEntity entity);

    OutboxPayload.ImpulseFilter toOutboxPayload(Request.ImpulseFilterDto dto);
}
