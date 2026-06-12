package ru.models;

import lombok.Data;

@Data
public class Order {

    private final long id;
    private final Type type;
    private final Side side;
    private final String symbol;
    private final long price;
    private long count;

    public void reduceCount(long quantity) {
        if (quantity > this.count) {
            throw new IllegalArgumentException("Cannot reduce order count below zero");
        }
        this.count -= quantity;
    }

    public boolean isFilled() {
        return this.count == 0;
    }
}