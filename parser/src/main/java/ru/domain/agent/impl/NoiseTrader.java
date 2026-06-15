package ru.domain.agent.impl;

import ru.domain.agent.AbstractTrader;
import ru.domain.agent.TraderType;
import ru.domain.market.Exchange;
import ru.domain.market.Order;
import ru.domain.market.Side;
import ru.domain.market.Type;
import ru.domain.simulation.SimulationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NoiseTrader extends AbstractTrader {
    private final double sigmaNT; // Общая активность (θ = σ_NT / N_NT)
    private final double rho;     // Соотношение market/limit (μ = θ * ρ)
    private final double muL;
    private final double sigmaL;

    public NoiseTrader(
            double sigmaNT, double rho, double delta,
            long balance, double muL, double sigmaL,
            List<String> symbols, Map<String, Double> targetUsdVolumes)
    {
        super(0.0, 0.0, delta, balance, symbols, targetUsdVolumes);
        this.sigmaNT = sigmaNT;
        this.rho = rho;
        this.muL = muL;
        this.sigmaL = sigmaL;
    }

    @Override
    public TraderType type() {
        return TraderType.NOISE;
    }

    @Override
    public List<Order> tick(Exchange market, SimulationContext context) {
        List<Order> newOrders = new ArrayList<>();
        long currentTick = context.getCurrentTick();
        for (String symbol : getSymbols()) {
            newOrders.addAll(tickForSymbol(market, symbol, currentTick, context));
        }
        return newOrders;
    }

    private List<Order> tickForSymbol(Exchange market, String symbol, long currentTick, SimulationContext context) {
        List<Order> newOrders = new ArrayList<>();

        cancelActiveOrders(market, symbol, getDelta());

        long marketPrice = market.getMidPrice(symbol);
        if (marketPrice == 0) return newOrders;

        long quantity = calculateOrderQuantity(marketPrice, symbol, 1.0);

        // Нормировка на число NT-агентов: θ = σ_NT / N_NT (Section 3.13)
        int nNt = context.getRegistry().getCount(TraderType.NOISE);
        double currentTheta = sigmaNT / Math.max(1, nNt);
        double currentMu = currentTheta * rho;

        // Лимитные ордера
        if (random.nextDouble() < currentTheta) {
            Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
            newOrders.add(generateLimitOrder(market, symbol, side, marketPrice, quantity, muL, sigmaL));
        }

        // Рыночные ордера
        if (random.nextDouble() < currentMu) {
            Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
            Order marketOrder = new Order(this, Type.MARKET, side, symbol, 0, quantity);
            newOrders.add(marketOrder);
        }
        return newOrders;
    }
}
