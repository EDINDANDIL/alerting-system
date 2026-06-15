package ru.models.factories;

import ru.models.Trader;
import ru.models.agents.FundamentalTrader;
import ru.models.agents.NoiseTrader;
import ru.models.agents.MomentumTrader;
import ru.models.agents.MarketMaker;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class TraderFactory {

    private static final long DEFAULT_BALANCE = 1_000_000_00000000L;
    // Баланс для маркет-мейкеров ($10,000,000 в масштабе 10^8)
    private static final long MM_BALANCE = 10_000_000_00000000L;

    public static List<Trader> fundamentalTrader(int count, List<String> symbols, Map<String, Long> targetValues, Map<String, Double> targetUsdVolumes) {
        return Stream.generate(() -> fundamentalTrader(targetValues, symbols, targetUsdVolumes))
                .limit(count)
                .collect(Collectors.toList());
    }

    public static Trader fundamentalTrader(Map<String, Long> targetValues, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return new FundamentalTrader(
                DEFAULT_BALANCE,
                1.5,  // kappa1 (линейный спрос)
                0.8,  // kappa2 (кубический спрос)
                5,    // interval (проверка раз в 5 тиков)
                symbols,
                targetValues,
                targetUsdVolumes
        );
    }

    public static List<Trader> momentumTrader(int count, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return IntStream.range(0, count)
                .mapToObj(i -> i % 2 == 0 ? shortTermMomentumTrader(symbols, targetUsdVolumes) : longTermMomentumTrader(symbols, targetUsdVolumes))
                .collect(Collectors.toList());
    }

    public static List<Trader> longTermMomentumTrader(int count, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return Stream.generate(() -> longTermMomentumTrader(symbols, targetUsdVolumes))
                .limit(count)
                .collect(Collectors.toList());
    }

    public static List<Trader> shortTermMomentumTrader(int count, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return Stream.generate(() -> shortTermMomentumTrader(symbols, targetUsdVolumes))
                .limit(count)
                .collect(Collectors.toList());
    }

    public static Trader longTermMomentumTrader(List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return new MomentumTrader(
                0.15, // theta
                0.05, // mu
                0.05, // delta
                DEFAULT_BALANCE,
                0.001, // alpha (затухание для LMT)
                1.2,  // beta
                10.0, // gamma
                0.5,  // rho
                0.005,
                0.002,
                symbols,
                targetUsdVolumes
        );
    }

    public static Trader shortTermMomentumTrader(List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return new MomentumTrader(
                0.15, // theta
                0.05, // mu
                0.05, // delta
                DEFAULT_BALANCE,
                0.9,  // alpha (затухание для SMT)
                1.2,  // beta
                10.0, // gamma
                0.5,  // rho
                0.005,
                0.002,
                symbols,
                targetUsdVolumes
        );
    }

    public static List<Trader> noiseTrader(int count, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return Stream.generate(() -> noiseTrader(symbols, targetUsdVolumes))
                .limit(count)
                .collect(Collectors.toList());
    }

    public static Trader noiseTrader(List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return new NoiseTrader(
                0.25, // theta
                0.15, // mu
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
                0.005, // pMmedge (0.5%)
                5000L, // limit
                1000L, // safe
                20L,    // cooldownTicks
                symbols,
                targetUsdVolumes
        );
    }
}