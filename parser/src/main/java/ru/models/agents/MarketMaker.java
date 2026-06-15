package ru.models.agents;

import ru.models.*;

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

    // Состояние и время паузы изолировано для каждой монеты
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
    public List<Order> tick(Exchange market, long currentTick) {
        List<Order> newOrders = new ArrayList<>();
        for (String symbol : getSymbols()) {
            newOrders.addAll(tickForSymbol(market, symbol, currentTick));
        }
        return newOrders;
    }

    private List<Order> tickForSymbol(Exchange market, String symbol, long currentTick) {
        List<Order> newOrders = new ArrayList<>();
        long inventory = getInventory(symbol);

        State state = states.getOrDefault(symbol, State.NORMAL);
        long suspensionEndTick = suspensionEndTicks.getOrDefault(symbol, 0L);

        // 1. Состояние паузы (Suspended)
        if (state == State.SUSPENDED) {
            if (currentTick >= suspensionEndTick) {
                state = State.NORMAL;
                states.put(symbol, State.NORMAL);
            } else {
                return newOrders; // Ничего не делаем
            }
        }

        // 2. Проверка перехода в стрессовое состояние из нормального
        if (state == State.NORMAL && Math.abs(inventory) >= limit) {
            state = State.STRESSED;
            states.put(symbol, State.STRESSED);
            // Снимаем все наши лимитки по этому символу из стакана
            cancelActiveOrders(market, symbol, 1.0);
        }

        long pt = market.getMidPrice(symbol);
        if (pt == 0) return newOrders;

        long quantity = calculateOrderQuantity(pt, symbol, 10.0);

        // 3. Состояние стресса (Экстренный сброс баланса рыночными ордерами)
        if (state == State.STRESSED) {
            if (Math.abs(inventory) <= safe) {
                state = State.SUSPENDED;
                states.put(symbol, State.SUSPENDED);
                suspensionEndTicks.put(symbol, currentTick + cooldownTicks);
            } else {
                // Если инвентарь избыточен (плюсовой) - продаем, если дефицит (минусовой) - покупаем
                Side side = inventory > 0 ? Side.SELL : Side.BUY;
                Order marketOrder = new Order(this, Type.MARKET, side, symbol, 0, quantity);
                newOrders.add(marketOrder);
            }
            return newOrders;
        }

        cancelActiveOrders(market, symbol, getDelta());

        // Выставляем котировку (BUY и SELL лимитные ордера одновременно)
        if (random.nextDouble() < getTheta()) {
            double d1 = random.nextDouble() * pMmedge;
            double d2 = random.nextDouble() * pMmedge;
            
            // Рассчитываем спред в процентах от текущей цены монеты
            long buyPrice = pt - Math.round(d1 * pt);
            long sellPrice = pt + Math.round(d2 * pt);

            // Округляем до tickSize монеты
            long tickSize = market.getTickSize(symbol);
            buyPrice = Math.round((double) buyPrice / tickSize) * tickSize;
            sellPrice = Math.round((double) sellPrice / tickSize) * tickSize;

            Order buyQuote = new Order(this, Type.LIMIT, Side.BUY, symbol, buyPrice, quantity);
            Order sellQuote = new Order(this, Type.LIMIT, Side.SELL, symbol, sellPrice, quantity);
            
            addOrder(buyQuote);
            addOrder(sellQuote);
            
            newOrders.add(buyQuote);
            newOrders.add(sellQuote);
        }
        return newOrders;
    }
}