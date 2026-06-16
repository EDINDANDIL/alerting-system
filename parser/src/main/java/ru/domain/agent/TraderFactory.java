package ru.domain.agent;

import ru.domain.agent.impl.FundamentalTrader;
import ru.domain.agent.impl.NoiseTrader;
import ru.domain.agent.impl.MomentumTrader;
import ru.domain.agent.impl.MarketMaker;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class TraderFactory {

    private static final long DEFAULT_BALANCE = 1_000_000_00000000L;
    // Баланс для маркет-мейкеров ($10,000,000 в масштабе 10^8)
    private static final long MM_BALANCE = 10_000_000_00000000L;

    public static List<Trader> fundamentalTrader(int count, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return Stream.generate(() -> fundamentalTrader(symbols, targetUsdVolumes))
                .limit(count)
                .collect(Collectors.toList());
    }

    public static Trader fundamentalTrader(List<String> symbols, Map<String, Double> targetUsdVolumes) {
        return new FundamentalTrader(
                DEFAULT_BALANCE,
                0.15,  // kappa1 (линейный спрос)
                0.45,  // kappa2 (кубический спрос)
                100,  // interval (проверка раз в 100 тиков, Table 7)
                symbols,
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
                0.01,  // alpha (затухание для LMT, Table 7)
                5.0,  // beta
                10.0, // gamma (масштабирует малые доходности за тик)
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
                5.0,  // beta
                10.0, // gamma (масштабирует малые доходности за тик)
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
                30.0, // sigmaNT (общая активность NT, нормируется на N_NT)
                0.85,  // rho (соотношение market/limit)
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
                0.0005, // pMmedge (0.5%)
                50000L, // limit
                101L,   // safe (ε_safe = 101, Table 7)
                5000L, // cooldownTicks (ε_rest = 12000, Table 7)
                symbols,
                targetUsdVolumes
        );
    }
}
