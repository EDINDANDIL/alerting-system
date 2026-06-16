package ru.domain.agent;

import ru.domain.agent.impl.FundamentalTrader;
import ru.domain.agent.impl.NoiseTrader;
import ru.domain.agent.impl.MomentumTrader;
import ru.domain.agent.impl.MarketMaker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class TraderFactory {

    private static final long DEFAULT_BALANCE = 1_000_000_00000000L;
    private static final long MM_BALANCE = 10_000_000_00000000L;

    public static List<Trader> fundamentalTrader(int count, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return IntStream.range(0, count)
                .mapToObj(_ -> new FundamentalTrader(
                        DEFAULT_BALANCE,
                        0.15,
                        0.45,
                        ThreadLocalRandom.current().nextLong(50, 151), // Размытые каскадные входы
                        symbols, targetUsdVolumes
                )).collect(Collectors.toList());
    }

    public static List<Trader> momentumTrader(int count, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return IntStream.range(0, count)
                .mapToObj(i -> i % 2 == 0 ? shortTermMomentumTrader(symbols, targetUsdVolumes) : longTermMomentumTrader(symbols, targetUsdVolumes))
                .collect(Collectors.toList());
    }

    public static List<Trader> longTermMomentumTrader(int count, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return IntStream.range(0, count)
                .mapToObj(_ -> new MomentumTrader(
                        0.15, // theta
                        0.05, // mu
                        0.05, // delta
                        DEFAULT_BALANCE,
                        0.005 + ThreadLocalRandom.current().nextDouble() * 0.045, // Гетерогенная альфа для LMT
                        5.0, // beta
                        10.0, // gamma
                        0.5, // rho
                        0.005, // muL
                        0.002, // sigmaL
                        symbols, targetUsdVolumes
                ))
                .collect(Collectors.toList());
    }

    public static List<Trader> shortTermMomentumTrader(int count, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return IntStream.range(0, count)
                .mapToObj(_ -> new MomentumTrader(
                        0.15, // theta
                        0.05, // mu
                        0.05, // delta
                        DEFAULT_BALANCE,
                        0.5 + ThreadLocalRandom.current().nextDouble() * 0.45, // Гетерогенная альфа для SMT
                        5.0, // beta
                        10.0, // gamma
                        0.5, // rho
                        0.005, // muL
                        0.002, // sigmaL
                        symbols, targetUsdVolumes
                ))
                .collect(Collectors.toList());
    }

    public static Trader longTermMomentumTrader(List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return new MomentumTrader(
                0.15, 0.05, 0.05, DEFAULT_BALANCE, 0.01, 5.0, 10.0, 0.5, 0.005, 0.002, symbols, targetUsdVolumes
        );
    }

    public static Trader shortTermMomentumTrader(List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return new MomentumTrader(
                0.15, 0.05, 0.05, DEFAULT_BALANCE, 0.9, 5.0, 10.0, 0.5, 0.005, 0.002, symbols, targetUsdVolumes
        );
    }

    public static List<Trader> noiseTrader(int count, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return Stream.generate(() -> noiseTrader(symbols, targetUsdVolumes))
                .limit(count)
                .collect(Collectors.toList());
    }

    public static Trader noiseTrader(List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return new NoiseTrader(
                15.0, // sigmaNT (сниженный шум для более узких свечей)
                0.85,  // rho
                0.05, // delta
                DEFAULT_BALANCE,
                0.005, // muL
                0.002,  // sigmaL
                symbols,
                targetUsdVolumes
        );
    }

    public static List<Trader> marketMaker(int count, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return Stream.generate(() -> marketMaker(symbols, targetUsdVolumes))
                .limit(count)
                .collect(Collectors.toList());
    }

    public static Trader marketMaker(List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return new MarketMaker(
                0.6,  // theta
                0.08, // delta
                MM_BALANCE,
                0.0001, // pMmedge (0.01% - очень плотный стакан, убирает разброс теней)
                50000L, // limit
                101L,   // safe
                5000L, // cooldownTicks
                symbols,
                targetUsdVolumes
        );
    }
}
