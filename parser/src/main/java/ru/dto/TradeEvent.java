package ru.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

@Json
public record TradeEvent(
        String type, // Always "trade"
        String symbol,
        long price,
        long timestampMs
) {}
