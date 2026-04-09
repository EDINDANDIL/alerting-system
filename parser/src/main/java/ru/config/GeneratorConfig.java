package ru.config;

import ru.tinkoff.kora.config.common.Config;
import ru.tinkoff.kora.config.common.extractor.ConfigValueExtractor;

public record GeneratorConfig(
        long startPrice,
        long volumeBase,
        double volatility,
        double trend,
        int spikeInterval,
        double spikeAmplitude
) {
    public static GeneratorConfig create(
            Config config,
            ConfigValueExtractor<Long> longExtractor,
            ConfigValueExtractor<Double> doubleExtractor,
            ConfigValueExtractor<Integer> intExtractor
    ) {
        long startPrice = longExtractor.extract(config.get("generator.startPrice"));
        long volumeBase = longExtractor.extract(config.get("generator.volumeBase"));
        double volatility = doubleExtractor.extract(config.get("generator.volatility"));
        double trend = doubleExtractor.extract(config.get("generator.trend"));
        int spikeInterval = intExtractor.extract(config.get("generator.spikeInterval"));
        double spikeAmplitude = doubleExtractor.extract(config.get("generator.spikeAmplitude"));
        return new GeneratorConfig(startPrice, volumeBase, volatility, trend, spikeInterval, spikeAmplitude);
    }
}
