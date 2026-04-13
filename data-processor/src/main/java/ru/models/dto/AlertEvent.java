package ru.models.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

import java.util.List;
import java.util.Set;

@Json
public record AlertEvent(
        Set<Integer> subscribers,
        List<String> exchange,
        List<String> market,
        String symbol,
        long timestampNs
){}
