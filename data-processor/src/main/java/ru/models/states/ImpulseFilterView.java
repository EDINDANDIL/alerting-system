package ru.models.states;

import ru.common.dto.OutboxPayload;

import java.util.Set;

// иммутабельная запись текущего состояния фильтра по этому action=IMPULSE
public record ImpulseFilterView(
        long filterId,
        OutboxPayload.ImpulseFilter payload,
        Set<Integer> subscribers
) {}