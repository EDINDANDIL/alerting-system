package ru.domain.market;

import lombok.Getter;
import lombok.ToString;
import ru.domain.agent.Trader;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@ToString
public class Order {

    private final long id;
    private final Trader trader;
    private final Type type;
    private final Side side;
    private final String symbol;
    private final long price;
    private long count;

    public Order(Trader trader, Type type, Side side, String symbol, long price, long count) {
        this.id = IdGenerator.id();
        this.trader = trader;
        this.type = type;
        this.side = side;
        this.symbol = symbol;
        this.price = price;
        this.count = count;
    }

    public void reduceCount(long quantity) {
        if (quantity > this.count) throw new IllegalArgumentException("Cannot reduce order count below zero");
        this.count -= quantity;
    }

    public boolean isFilled() {
        return this.count == 0;
    }

    public static class IdGenerator {
        private final static AtomicLong counter = new AtomicLong(1);
        public static long id() {
            return counter.getAndIncrement();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id == order.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
