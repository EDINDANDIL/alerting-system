package ru.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

@Json
public record SimulatorStatusDto(
        boolean running,
        long currentTick,
        long totalTrades,
        double marketPrice,
        double bestBid,
        double bestAsk,
        long tickDelayMs,
        long startPrice,
        int noiseTradersCount,
        int momentumTradersCount,
        int fundamentalTradersCount,
        int marketMakersCount
) {}
