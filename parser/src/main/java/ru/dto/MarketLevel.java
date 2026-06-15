package ru.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

@Json
public record MarketLevel(
        long price,
        long quantity
) {}