package ru.models;

import java.util.Optional;

public class AbstractAgent implements Agent{
    private double theta;
    private double mu;

    @Override
    public long id() {
        return 0;
    }

    @Override
    public Optional<Order> tick(Exchange market, long currentTick) {
        return Optional.empty();
    }

    @Override
    public void onOrderFilled(Order order, long price, long quantity) {

    }
}
