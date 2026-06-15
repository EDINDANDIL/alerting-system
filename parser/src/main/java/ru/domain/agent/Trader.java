package ru.domain.agent;

import ru.domain.market.Exchange;
import ru.domain.market.Order;
import ru.domain.simulation.SimulationContext;

import java.util.List;

public interface Trader {

    long id();

    TraderType type();

    List<Order> tick(Exchange market, SimulationContext context);

    void onOrderFilled(Order order, long price, long quantity);
}
