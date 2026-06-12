package ru.models;

import java.util.Optional;

public interface Agent {

    long id();
    Optional<Order> tick(Exchange market, long currentTick);
    void onOrderFilled(Order order, long price, long quantity);
}
