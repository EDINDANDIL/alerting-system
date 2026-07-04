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

public class MarketMaker extends AbstractTrader {
    private enum State { NORMAL, STRESSED, SUSPENDED }

    private final double pMmedge;       // Максимальный разброс цены
    private final long limit;           // Предельный инвентарь риска
    private final long safe;            // Безопасный инвентарь
    private final long cooldownTicks;   // Время паузы в тиках

    private final Map<String, State> states = new HashMap<>();
    private final Map<String, Long> suspensionEndTicks = new HashMap<>();

    public MarketMaker(
            double theta, double delta, long balance,
            double pMmedge, long limit, long safe, long cooldownTicks,
            List<String> symbols, Map<String, Double> targetUsdVolumes)
    {
        super(theta, 0.0, delta, balance, symbols, targetUsdVolumes); // Во время нормальной торговли mu = 0
        this.pMmedge = pMmedge;
        this.limit = limit;
        this.safe = safe;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public TraderType type() {
        return TraderType.MARKET_MAKER;
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
        List<Order> newOrders = new ArrayList<>(); long inventory = getInventory(symbol);
        State state = states.getOrDefault(symbol, State.NORMAL);
        long suspensionEndTick = suspensionEndTicks.getOrDefault(symbol, 0L);
        if (state == State.SUSPENDED) {
            if (currentTick >= suspensionEndTick) {
                state = State.NORMAL;
                states.put(symbol, State.NORMAL);
            } else return newOrders;}
        if (state == State.NORMAL && Math.abs(inventory) >= limit) {
            state = State.STRESSED;
            states.put(symbol, State.STRESSED);
            cancelActiveOrders(market, symbol, 1.0);}
        long pt = market.getMidPrice(symbol); boolean bookEmpty = (pt == 0);
        if (bookEmpty) {
            pt = context.getFundamentalPrice(symbol);
            if (pt <= 0) return newOrders;}
        long quantity = calculateOrderQuantity(pt, symbol, 10.0);
        if (state == State.STRESSED) {
            if (Math.abs(inventory) <= safe) {
                states.put(symbol, State.SUSPENDED);
                suspensionEndTicks.put(symbol, currentTick + cooldownTicks);
                return newOrders;
            } else {
                if (!bookEmpty) {
                    Side side = inventory > 0 ? Side.SELL : Side.BUY;
                    Order marketOrder = new Order(this, Type.MARKET, side, symbol, 0, quantity);
                    newOrders.add(marketOrder);
                }}
            if (bookEmpty) newOrders.addAll(placeQuotes(market, symbol, pt, quantity));
            return newOrders;}
        cancelActiveOrders(market, symbol, getDelta());
        if (bookEmpty || random.nextDouble() < getTheta()) newOrders.addAll(placeQuotes(market, symbol, pt, quantity));
        return newOrders;
    }

    /**
     * Выставляет двустороннюю котировку (BUY + SELL) вокруг опорной цены.
     */
    private List<Order> placeQuotes(Exchange market, String symbol, long referencePrice, long quantity) {
        List<Order> orders = new ArrayList<>();

        double d1 = random.nextDouble() * pMmedge;
        double d2 = random.nextDouble() * pMmedge;

        long buyPrice = referencePrice - Math.round(d1 * referencePrice);
        long sellPrice = referencePrice + Math.round(d2 * referencePrice);

        // Округляем до tickSize монеты
        long tickSize = market.getTickSize(symbol);
        buyPrice = Math.round((double) buyPrice / tickSize) * tickSize;
        sellPrice = Math.round((double) sellPrice / tickSize) * tickSize;

        Order buyQuote = new Order(this, Type.LIMIT, Side.BUY, symbol, buyPrice, quantity);
        Order sellQuote = new Order(this, Type.LIMIT, Side.SELL, symbol, sellPrice, quantity);

        addOrder(buyQuote);
        addOrder(sellQuote);

        orders.add(buyQuote);
        orders.add(sellQuote);
        return orders;
    }
}