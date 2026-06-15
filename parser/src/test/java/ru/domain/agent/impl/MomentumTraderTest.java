package ru.domain.agent.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.domain.market.Exchange;
import ru.domain.market.Order;
import ru.domain.market.Side;
import ru.domain.simulation.SimulationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class MomentumTraderTest {
    private Exchange market;
    private MomentumTrader trader;

    private static final String SYMBOL = "BTC";

    @BeforeEach
    void setUp() {
        market = Mockito.mock(Exchange.class);
        trader = new MomentumTrader(
                1.0,         // theta
                1.0,         // mu
                0.0,         // delta
                1_000_000L,  // balance
                0.9,         // alpha
                2.0,         // beta
                10.0,        // gamma
                0.5,         // rho
                0.0,         // muL
                0.1,         // sigmaL
                List.of(SYMBOL),
                Map.of(SYMBOL, 1.0)
        );
    }

    @Test
    void testBuyOrderOnPositiveTrend() {
        // Price goes up: 60000 -> 61000
        when(market.getMidPrice(SYMBOL)).thenReturn(60000L * 100_000_000L);
        SimulationContext context1 = Mockito.mock(SimulationContext.class);
        when(context1.getCurrentTick()).thenReturn(1L);
        trader.tick(market, context1); // Set initial price

        when(market.getMidPrice(SYMBOL)).thenReturn(61000L * 100_000_000L);
        SimulationContext context2 = Mockito.mock(SimulationContext.class);
        when(context2.getCurrentTick()).thenReturn(2L);
        List<Order> orders = List.of();
        for (int i = 0; i < 100; i++) {
            orders = trader.tick(market, context2);
            if (!orders.isEmpty()) break;
        }

        assertFalse(orders.isEmpty(), "Should place order on positive trend");
        Order order = orders.get(0);
        assertEquals(Side.BUY, order.getSide(), "Positive trend should trigger BUY");
    }

    @Test
    void testSellOrderOnNegativeTrend() {
        // Price goes down: 60000 -> 59000
        when(market.getMidPrice(SYMBOL)).thenReturn(60000L * 100_000_000L);
        SimulationContext context1 = Mockito.mock(SimulationContext.class);
        when(context1.getCurrentTick()).thenReturn(1L);
        trader.tick(market, context1); // Set initial price

        when(market.getMidPrice(SYMBOL)).thenReturn(59000L * 100_000_000L);
        SimulationContext context2 = Mockito.mock(SimulationContext.class);
        when(context2.getCurrentTick()).thenReturn(2L);
        List<Order> orders = List.of();
        for (int i = 0; i < 100; i++) {
            orders = trader.tick(market, context2);
            if (!orders.isEmpty()) break;
        }

        assertFalse(orders.isEmpty(), "Should place order on negative trend");
        Order order = orders.get(0);
        assertEquals(Side.SELL, order.getSide(), "Negative trend should trigger SELL");
    }
}
