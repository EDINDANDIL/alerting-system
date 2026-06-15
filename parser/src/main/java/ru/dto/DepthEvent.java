package ru.dto;

import ru.tinkoff.kora.json.common.annotation.Json;
import java.util.List;

@Json
public record DepthEvent(
        String type, // Always "depth"
        String symbol,
        List<MarketLevel> bids,
        List<MarketLevel> asks,
        boolean running,
        long currentTick
) {}
