package ru.domain.agent;

import lombok.Getter;
import ru.domain.market.Exchange;
import ru.domain.market.Order;
import ru.domain.market.Side;
import ru.domain.market.Type;
import ru.domain.simulation.SimulationContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractTrader implements Trader {

    @Getter private final long id;
    @Getter private final double theta;
    @Getter private final double mu;
    @Getter private final double delta;
    protected final Random random = new Random();

    private final List<Order> orders = new CopyOnWriteArrayList<>();
    private final Map<String, Long> inventory = new ConcurrentHashMap<>();
    @Getter private final List<String> symbols;
    private final Map<String, Double> targetUsdVolumes;
    @Getter private volatile long balance;

    @Override
    public long id() {return id;}

    @Override
    public abstract TraderType type();

    public List<Order> orders() {return orders;}

    @Override
    public List<Order> tick(Exchange exchange, SimulationContext context) {
        return List.of();
    }

    public AbstractTrader(double theta, double mu, double delta, long balance, List<String> symbols, Map<String, Double> targetUsdVolumes) {
        this.id = IdGenerator.id();
        this.theta = theta;
        this.mu = mu;
        this.delta = delta;
        this.balance = balance;
        this.symbols = symbols;
        this.targetUsdVolumes = targetUsdVolumes;
    }

    public double getTargetUsdVolume(String symbol) {
        return targetUsdVolumes.getOrDefault(symbol, 5000.0);
    }

    @Override
    public synchronized void onOrderFilled(Order order, long price, long quantity) {
        String symbol = order.getSymbol();
        long currentAssetCount = getInventory(symbol);

        if (order.getSide() == Side.BUY) {
            this.balance -= (price * quantity);
            this.inventory.put(symbol, currentAssetCount + quantity);
        } else {
            this.balance += (price * quantity);
            this.inventory.put(symbol, currentAssetCount - quantity);
        }
        if (order.isFilled()) orders.remove(order);
    }

    public long getInventory(String symbol) {
        return inventory.getOrDefault(symbol, 0L);
    }

    public synchronized void addOrder(Order order) {orders.add(order);}

    public static class IdGenerator {
        private final static AtomicLong counter = new AtomicLong(1);
        public static long id() {return counter.getAndIncrement();}
    }
    protected void cancelActiveOrders(Exchange market, String symbol, double probability) {
        List<Order> toCancel = new ArrayList<>();
        for (Order order : orders()) {
            if (order.getSymbol().equals(symbol)) {
                if (probability >= 1.0 || random.nextDouble() < probability) {
                    toCancel.add(order);
                }
            }
        }
        for (Order order : toCancel) {
            market.cancel(order);
            orders().remove(order);
        }
    }

    protected long calculateOrderQuantity(long midPrice, String symbol, double volumeMultiplier) {
        if (midPrice == 0) return 0;
        double targetVolume = getTargetUsdVolume(symbol) * volumeMultiplier;
        return Math.max(1, Math.round(targetVolume / ((double) midPrice / 100_000_000.0)));
    }
    protected Order generateLimitOrder(Exchange market, String symbol, Side side, long midPrice, long quantity, double muL, double sigmaL) {
        double x = muL + sigmaL * random.nextGaussian();
        long distance = Math.round(Math.exp(x) * midPrice / 50000.0);
        long price = (side == Side.BUY) ? (midPrice - distance) : (midPrice + distance);

        long tickSize = market.getTickSize(symbol);
        price = Math.round((double) price / tickSize) * tickSize;

        Order limitOrder = new Order(this, Type.LIMIT, side, symbol, price, quantity);
        addOrder(limitOrder);
        return limitOrder;
    }
}
