package ru.models.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

import java.util.List;
import java.util.Set;

@Json
public record AlertEvent(
        Set<Long> subscribers,
        Set<String> exchange,
        Set<String> market,
        String symbol,
        long timestampNs
){}
