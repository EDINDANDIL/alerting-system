package ru.domain.agent.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.domain.agent.AgentRegistry;
import ru.domain.agent.TraderType;
import ru.domain.market.Exchange;
import ru.domain.market.Order;
import ru.domain.market.Side;
import ru.domain.market.Type;
import ru.domain.simulation.SimulationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FundamentalTraderTest {
    private Exchange market;
    private FundamentalTrader trader;

    private static final String SYMBOL = "BTC";
    private static final long TARGET_PRICE_SCALED = 60000L * 100_000_000L; // $60,000 scaled 10^8

    @BeforeEach
    void setUp() {
        market = Mockito.mock(Exchange.class);
        trader = new FundamentalTrader(
                1_000_000_000L, // balance
                2.0,            // kappa1 (linear demand coeff)
                1.5,            // kappa2 (cubic demand coeff)
                5,              // interval (ticks)
                List.of(SYMBOL),
                Map.of(SYMBOL, 1.0) // targetUsdVolume
        );
    }

    @Test
    void testTickIntervalRefusal() {
        SimulationContext context = Mockito.mock(SimulationContext.class);
        when(context.getCurrentTick()).thenReturn(3L);
        // Fundamental trader only executes tick on matching interval step (currentTick % interval == 0)
        List<Order> orders = trader.tick(market, context); // 3 % 5 != 0
        assertTrue(orders.isEmpty(), "Should not trade outside interval");
    }

    @Test
    void testBuyOrderWhenUnderpriced() {
        SimulationContext context = Mockito.mock(SimulationContext.class);
        AgentRegistry registry = Mockito.mock(AgentRegistry.class);
        when(registry.getCount(TraderType.FUNDAMENTAL)).thenReturn(1);
        when(context.getRegistry()).thenReturn(registry);
        when(context.getCurrentTick()).thenReturn(5L);
        when(context.getFundamentalPrice(SYMBOL)).thenReturn(TARGET_PRICE_SCALED);
        // BTC is underpriced ($50,000 instead of $60,000)
        when(market.getMidPrice(SYMBOL)).thenReturn(50000L * 100_000_000L);

        // Since the demand calculation includes a random check, we try to force execution or loop
        List<Order> orders = List.of();
        for (int i = 0; i < 100; i++) {
            orders = trader.tick(market, context); // 5 % 5 == 0
            if (!orders.isEmpty()) {
                break;
            }
        }

        assertFalse(orders.isEmpty(), "Should place order when mispriced");
        Order order = orders.get(0);
        assertEquals(Type.MARKET, order.getType());
        assertEquals(Side.BUY, order.getSide());
        assertEquals(SYMBOL, order.getSymbol());
    }

    @Test
    void testSellOrderWhenOverpriced() {
        SimulationContext context = Mockito.mock(SimulationContext.class);
        AgentRegistry registry = Mockito.mock(AgentRegistry.class);
        when(registry.getCount(TraderType.FUNDAMENTAL)).thenReturn(1);
        when(context.getRegistry()).thenReturn(registry);
        when(context.getCurrentTick()).thenReturn(5L);
        when(context.getFundamentalPrice(SYMBOL)).thenReturn(TARGET_PRICE_SCALED);
        // BTC is overpriced ($70,000 instead of $60,000)
        when(market.getMidPrice(SYMBOL)).thenReturn(70000L * 100_000_000L);

        List<Order> orders = List.of();
        for (int i = 0; i < 100; i++) {
            orders = trader.tick(market, context);
            if (!orders.isEmpty()) {
                break;
            }
        }

        assertFalse(orders.isEmpty(), "Should place order when mispriced");
        Order order = orders.get(0);
        assertEquals(Type.MARKET, order.getType());
        assertEquals(Side.SELL, order.getSide());
        assertEquals(SYMBOL, order.getSymbol());
    }
}
