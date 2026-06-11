package ru.common.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

import java.util.Set;

@Json
public record AlertCreatedEvent(
        long filterId,
        Set<Long> subscribers,
        Set<String> exchange,
        Set<String> market,
        String symbol,
        long timestampNs
){}