package ru.util;

import ru.models.dto.FilterType;
import ru.services.FilterService;
import ru.services.ImpulseService;
import ru.tinkoff.kora.common.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FilterServiceRegistry {

    private final ImpulseService impulseService;
    private List<FilterService> allFilterServices = new ArrayList<>();

    public FilterServiceRegistry(ImpulseService impulseService) {
        this.impulseService = impulseService;
        allFilterServices.add(impulseService);
    }

    public FilterService getService(FilterType type) {
        return switch (type) {
            case IMPULSE -> impulseService;
            default -> throw new IllegalArgumentException("Unknown request type: " + type.getClass());
        };
    }

    public List<FilterService> allFilterServices() {return allFilterServices;}
}
