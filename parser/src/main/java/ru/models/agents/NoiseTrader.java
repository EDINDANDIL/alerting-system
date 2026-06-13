package ru.models.agents;

import ru.models.*;

import java.util.ArrayList;
import java.util.List;

public class NoiseTrader extends AbstractTrader implements Trader {
    private final double muL;
    private final double sigmaL;
    private final List<String> symbols;

    public NoiseTrader(
            double theta,
            double mu,
            double delta,
            long balance,
            double muL,
            double sigmaL,
            List<String> symbols)
    {
        super(theta, mu, delta, balance);
        this.muL = muL;
        this.sigmaL = sigmaL;
        this.symbols = symbols;
    }

    @Override
    public List<Order> tick(Exchange market, long currentTick) {
        List<Order> newOrders = new ArrayList<>();
        for (String symbol : symbols) {
            newOrders.addAll(tickForSymbol(market, symbol, currentTick));
        }
        return newOrders;
    }

    private List<Order> tickForSymbol(Exchange market, String symbol, long currentTick) {
        List<Order> newOrders = new ArrayList<>();

        // 1. Отмена старых ордеров для данного символа
        orders().removeIf(order -> {
            if (order.getSymbol().equals(symbol) && random.nextDouble() < getDelta()) {
                market.cancel(order);
                return true;
            }
            return false;
        });

        long marketPrice = market.getMarketPrice(symbol);
        if (marketPrice == 0) return newOrders;

        // Рассчитываем объем ордера (целевой объем сделки $5,000)
        long quantity = Math.max(1, Math.round(5000.0 / ((double) marketPrice / 100_000_000.0)));

        // 2. Лимитные ордера
        if (random.nextDouble() < getTheta()) {
            Side side = random.nextBoolean() ? Side.BUY : Side.SELL;
            double x = muL + sigmaL * random.nextGaussian();
            // Масштабируем абсолютную дистанцию ($1 в калибровке) пропорционально цене актива
            long distance = Math.round(Math.exp(x) * marketPrice / 50000.0);
            
            long price = (side == Side.BUY) ? (marketPrice - distance) : (marketPrice + distance);
            
            // Округляем до tickSize
            long tickSize = market.getTickSize(symbol);
            price = Math.round((double) price / tickSize) * tickSize;

            Order limitOrder = new Order(this, Type.LIMIT, side, symbol, price, quantity);
            addOrder(limitOrder);
            newOrders.add(limitOrder);
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