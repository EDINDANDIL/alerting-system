package ru.domain.agent.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.domain.market.Exchange;
import ru.domain.market.Order;
import ru.domain.market.Side;
import ru.domain.market.Type;
import ru.domain.simulation.SimulationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class MarketMakerTest {
    private Exchange market;
    private MarketMaker mm;

    private static final String SYMBOL = "BTC";
    private static final long LIMIT = 5000L;
    private static final long SAFE = 1000L;
    private static final long COOLDOWN = 20L;

    @BeforeEach
    void setUp() {
        market = Mockito.mock(Exchange.class);
        mm = new MarketMaker(
                1.0,          // theta (quote probability = 100% for test)
                0.05,         // delta (cancel probability)
                1000000000L,  // balance
                0.005,        // pMmedge
                LIMIT,
                SAFE,
                COOLDOWN,
                List.of(SYMBOL),
                Map.of(SYMBOL, 1.0)
        );
    }

    @Test
    void testNormalQuoting() {
        when(market.getMidPrice(SYMBOL)).thenReturn(60000L * 100_000_000L);
        when(market.getTickSize(SYMBOL)).thenReturn(1000L);

        SimulationContext context = Mockito.mock(SimulationContext.class);
        when(context.getCurrentTick()).thenReturn(1L);

        List<Order> orders = mm.tick(market, context);

        // Under normal quoting, a market maker submits exactly one BUY limit order and one SELL limit order
        assertEquals(2, orders.size(), "Market maker should place two quotes");
        
        Order buy = orders.stream().filter(o -> o.getSide() == Side.BUY).findFirst().orElse(null);
        Order sell = orders.stream().filter(o -> o.getSide() == Side.SELL).findFirst().orElse(null);

        assertNotNull(buy);
        assertNotNull(sell);

        assertEquals(Type.LIMIT, buy.getType());
        assertEquals(Type.LIMIT, sell.getType());
        
        assertTrue(buy.getPrice() < sell.getPrice(), "Buy price must be lower than sell price");
    }

    @Test
    void testStressedInventoryClearing() {
        when(market.getMidPrice(SYMBOL)).thenReturn(60000L * 100_000_000L);
        
        // Simulating that the market maker holds too much long inventory (6000 >= LIMIT of 5000)
        mm.onOrderFilled(new Order(mm, Type.LIMIT, Side.BUY, SYMBOL, 60000L, 6000), 60000L, 6000);
        assertTrue(mm.getInventory(SYMBOL) >= LIMIT, "Inventory should exceed limit");

        SimulationContext context = Mockito.mock(SimulationContext.class);
        when(context.getCurrentTick()).thenReturn(2L);

        // First tick in stressed state: should cancel quotes and submit sell market order to clear inventory
        List<Order> orders = mm.tick(market, context);

        assertFalse(orders.isEmpty(), "Stressed MM should submit orders to clear inventory");
        Order order = orders.getFirst();
        assertEquals(Type.MARKET, order.getType());
        assertEquals(Side.SELL, order.getSide(), "Should SELL to reduce long inventory position");
    }
}
