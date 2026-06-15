package ru.config;

import jakarta.annotation.Nullable;
import ru.tinkoff.kora.config.common.annotation.ConfigValueExtractor;

import java.util.List;
import java.util.Map;

@ConfigValueExtractor
public record SimulatorConfig(
        long tickDelayMs,
        long ticks,
        List<String> symbols,
        Map<String, Long> startPrices,
        Map<String, Long> tickSizes,
        Map<String, Double> targetUsdVolumes,
        int noiseTradersCount,
        int momentumTradersCount,
        int fundamentalTradersCount,
        int marketMakersCount
) {}