package ru.models.agents;

import ru.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NoiseTrader extends AbstractTrader {
    private final double muL;
    private final double sigmaL;

    public NoiseTrader(
            double theta, double mu, double delta,
            long balance, double muL, double sigmaL,
            List<String> symbols, Map<String, Double> targetUsdVolumes)
    {
        super(theta, mu, delta, balance, symbols, targetUsdVolumes);
        this.muL = muL;
        this.sigmaL = sigmaL;
    }

    @Override
    public List<Order> tick(Exchange market, long currentTick) {
        List<Order> newOrders = new ArrayList<>();
        for (String symbol : getSymbols()) {
            newOrders.addAll(tickForSymbol(market, symbol, currentTick));
        }
        return newOrders;
    }

    private List<Order> tickForSymbol(Exchange market, String symbol, long currentTick) {
        List<Order> newOrders = new ArrayList<>();

        cancelActiveOrders(market, symbol, getDelta());

        long marketPrice = market.getMidPrice(symbol);
        if (marketPrice == 0) return newOrders;

        long quantity = calculateOrderQuantity(marketPrice, symbol, 1.0);

        // 2. Лимитные ордера
        if (random.nextDouble() < getTheta()) {
            Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
            newOrders.add(generateLimitOrder(market, symbol, side, marketPrice, quantity, muL, sigmaL));
        }

        // 3. Рыночные ордера
        if (random.nextDouble() < getMu()) {
            Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
            Order marketOrder = new Order(this, Type.MARKET, side, symbol, 0, quantity);
            newOrders.add(marketOrder);
        }
        return newOrders;
    }
}