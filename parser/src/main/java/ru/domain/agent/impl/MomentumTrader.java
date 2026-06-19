package ru.domain.agent.impl;

import ru.domain.agent.AbstractTrader;
import ru.domain.agent.TraderType;
import ru.domain.market.Exchange;
import ru.domain.market.Order;
import ru.domain.market.Side;
import ru.domain.market.Type;
import ru.domain.simulation.SimulationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MomentumTrader extends AbstractTrader {
    private final double alpha; // Скорость затухания тренда (0.001 для LMT / 0.9 для SMT)
    private final double beta;  // Коэффициент спроса
    private final double gamma; // Насыщение (обычно 10.0)
    private final double rho;   // Соотношение рынок/лимит
    private final double muL;
    private final double sigmaL;

    private final Map<String, Double> mtMap = new HashMap<>();
    private final Map<String, Long> lastPriceMap = new HashMap<>();

    public MomentumTrader(
            double theta, double mu, double delta, long balance,
            double alpha, double beta, double gamma, double rho, double muL, double sigmaL,
            List<String> symbols, Map<String, Double> targetUsdVolumes)
    {
        super(theta, mu, delta, balance, symbols, targetUsdVolumes);
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.rho = rho;
        this.muL = muL;
        this.sigmaL = sigmaL;
    }

    @Override
    public TraderType type() {
        return TraderType.MOMENTUM;
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
        long pt = market.getMidPrice(symbol);

        cancelActiveOrders(market, symbol, getDelta());

        if (pt == 0) return newOrders;

        long quantity = calculateOrderQuantity(pt, symbol, 1.0);
        
        double mt = mtMap.getOrDefault(symbol, 0.0);
        long lastPrice = lastPriceMap.getOrDefault(symbol, 0L);

        if (lastPrice != 0) mt = (1 - alpha) * mt + alpha * ((double) (pt - lastPrice) / lastPrice);

        lastPrice = pt;
        mtMap.put(symbol, mt);
        lastPriceMap.put(symbol, lastPrice);

        double demand = beta * Math.tanh(gamma * mt * 100.0);
        double currentTheta = Math.abs(demand);
        double currentMu = currentTheta * rho;

        Side side = mt >= 0 ? Side.BUY : Side.SELL;

        if (random.nextDouble() < currentTheta) {
            newOrders.add(generateLimitOrder(market, symbol, side, pt, quantity, muL, sigmaL));
        }

        if (random.nextDouble() < currentMu) {
            Order marketOrder = new Order(this, Type.MARKET, side, symbol, 0, quantity);
            newOrders.add(marketOrder);
        }
        return newOrders;
    }
}