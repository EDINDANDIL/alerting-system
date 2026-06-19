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

public class FundamentalTrader extends AbstractTrader {
    private final double kappa1;     // Коэффициент линейного спроса
    private final double kappa2;     // Коэффициент кубического спроса
    private final long interval;     // Как часто ходит (например, 100 тиков)

    public FundamentalTrader(
            long balance, double kappa1, double kappa2, long interval,
            List<String> symbols,
            Map<String, Double> targetUsdVolumes)
    {
        super(0.0, 0.0, 0.0, balance, symbols, targetUsdVolumes);
        this.kappa1 = kappa1;
        this.kappa2 = kappa2;
        this.interval = interval;
    }

    @Override
    public TraderType type() {
        return TraderType.FUNDAMENTAL;
    }

    @Override
    public List<Order> tick(Exchange market, SimulationContext context) {

        long currentTick = context.getCurrentTick();

        if (currentTick % interval != 0) return List.of();

        List<Order> newOrders = new ArrayList<>();

        for (String symbol : getSymbols()) {
            long targetValue = context.getFundamentalPrice(symbol);
            if (targetValue <= 0) continue;

            long pt = market.getMidPrice(symbol);
            if (pt == 0) continue;

            long quantity = calculateOrderQuantity(pt, symbol, 1.0);

            double diff = ((double) (targetValue - pt) / targetValue) * 100.0;
            
            double demand = kappa1 * diff + kappa2 * Math.pow(diff, 3);
            int nFt = context.getRegistry().getCount(TraderType.FUNDAMENTAL);
            double currentMu = Math.abs(demand) / Math.max(1, nFt);

            if (random.nextDouble() < currentMu) {
                Side side = diff > 0 ? Side.BUY : Side.SELL;
                Order marketOrder = new Order(this, Type.MARKET, side, symbol, 0, quantity);
                newOrders.add(marketOrder);
            }
        }
        return newOrders;
    }
}