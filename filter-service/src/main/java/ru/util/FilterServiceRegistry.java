package ru.util;

import ru.models.dto.FilterType;
import ru.services.FilterService;
import ru.services.ImpulseService;
import ru.tinkoff.kora.common.Component;

@Component
public class FilterServiceRegistry {

    private final ImpulseService impulseService;

    public FilterServiceRegistry(ImpulseService impulseService) {
        this.impulseService = impulseService;
    }

    public FilterService getService(FilterType type) {
        return switch (type) {
            case IMPULSE -> impulseService;
            default -> throw new IllegalArgumentException("Unknown request type: " + type.getClass());
        };
    }
}
