package ru.domain.agent.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.domain.agent.AgentRegistry;
import ru.domain.agent.TraderType;
import ru.domain.market.Exchange;
import ru.domain.market.Order;
import ru.domain.market.Type;
import ru.domain.simulation.SimulationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class NoiseTraderTest {
    private Exchange market;
    private NoiseTrader trader;

    private static final String SYMBOL = "BTC";

    @BeforeEach
    void setUp() {
        market = Mockito.mock(Exchange.class);
        // sigmaNT=1.0 с N_NT=1 даёт theta=1.0; rho=1.0 даёт mu=1.0
        trader = new NoiseTrader(
                1.0,         // sigmaNT
                1.0,         // rho
                0.0,         // delta
                1_000_000L,  // balance
                0.0,         // muL
                0.1,         // sigmaL
                List.of(SYMBOL),
                Map.of(SYMBOL, 1.0)
        );
    }

    @Test
    void testNoiseTraderOrders() {
        when(market.getMidPrice(SYMBOL)).thenReturn(60000L * 100_000_000L);

        SimulationContext context = Mockito.mock(SimulationContext.class);
        AgentRegistry registry = Mockito.mock(AgentRegistry.class);
        when(registry.getCount(TraderType.NOISE)).thenReturn(1);
        when(context.getRegistry()).thenReturn(registry);
        when(context.getCurrentTick()).thenReturn(1L);

        List<Order> orders = trader.tick(market, context);

        // Since sigmaNT/N_NT=1.0 and rho=1.0, noise trader should place both a limit order and a market order
        assertEquals(2, orders.size());
        assertTrue(orders.stream().anyMatch(o -> o.getType() == Type.LIMIT));
        assertTrue(orders.stream().anyMatch(o -> o.getType() == Type.MARKET));
    }
}

