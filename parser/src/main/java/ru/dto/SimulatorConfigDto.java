package ru.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

@Json
public record SimulatorConfigDto(
        Long tickDelayMs,
        Long startPrice,
        Integer noiseTradersCount,
        Integer momentumTradersCount,
        Integer fundamentalTradersCount,
        Integer marketMakersCount
) {}