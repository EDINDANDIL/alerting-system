package ru.core;

import ru.tinkoff.kora.application.graph.All;
import ru.tinkoff.kora.common.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public final class FilterEventHandlerRegistry {

    private final Map<String, FilterEventHandler> handlers;

    public FilterEventHandlerRegistry(All<FilterEventHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(FilterEventHandler::action, h -> h));
    }

    public FilterEventHandler get(String action) {
        FilterEventHandler handler = handlers.get(action);
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported action: " + action);
        }
        return handler;
    }
}
