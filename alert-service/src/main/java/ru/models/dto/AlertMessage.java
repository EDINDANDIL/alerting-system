package ru.models.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

import java.util.List;

@Json
public record AlertMessage(
        List<String> exchange,
        List<String> market,
        String symbol,
        long timestampNs
) {}
