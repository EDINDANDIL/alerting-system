package ru.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

@Json
public record TradeEvent(
        String type,
        String symbol,
        long price,
        long timestampMs
) {}
