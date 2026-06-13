package ru.models;

import java.util.List;

public interface Trader {

    long id();
    List<Order> tick(Exchange market, long currentTick);
    void onOrderFilled(Order order, long price, long quantity);
}
