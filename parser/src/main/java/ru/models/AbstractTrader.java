package ru.models;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class AbstractTrader implements Trader {

    @Getter private final long id;
    @Setter @Getter private double theta;
    @Getter @Setter private double mu;
    @Getter @Setter private double delta;
    protected final Random random = new Random();

    private final List<Order> orders = new ArrayList<>();
    private final Map<String , Long> inventory = new HashMap<>();
    @Getter private final List<String> symbols = new ArrayList<>();
    @Getter private long balance;

    @Override
    public long id() {return id;}

    public List<Order> orders() {return orders;}

    @Override
    public List<Order> tick(Exchange exchange, long currentTick) {
        return List.of();
    }

    public AbstractTrader(double theta, double mu, double delta, long balance) {
        this.id = IdGenerator.id();
        this.theta = theta;
        this.mu = mu;
        this.delta = delta;
        this.balance = balance;
    }

    @Override
    public void onOrderFilled(Order order, long price, long quantity) {
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

    public void addOrder(Order order) {orders.add(order);}

    public static class IdGenerator {
        private final static AtomicLong counter = new AtomicLong(1);
        public static long id() {return counter.getAndIncrement();}
    }
}