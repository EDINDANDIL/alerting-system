package ru.util;

import ru.models.dto.Request;
import ru.services.FilterService;
import ru.services.ImpulseService;
import ru.tinkoff.kora.common.Component;

@Component
public class FilterServiceRegistry {

    private final ImpulseService impulseService;

    public FilterServiceRegistry(ImpulseService impulseService) {
        this.impulseService = impulseService;
    }

    public FilterService getService(Request req) {
        return switch (req) {
            case Request.ImpulseFilterDto dto -> impulseService;
            default -> throw new IllegalArgumentException("Unknown request type: " + req.getClass());
        };
    }

}
