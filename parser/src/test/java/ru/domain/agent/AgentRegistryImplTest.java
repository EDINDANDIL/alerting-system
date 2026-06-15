package ru.domain.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.domain.market.Exchange;
import ru.domain.market.Order;
import ru.domain.simulation.SimulationContext;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgentRegistryImplTest {
    private AgentRegistryImpl registry;

    @BeforeEach
    void setUp() {
        registry = new AgentRegistryImpl();
    }

    @Test
    void testInitialCountsAreZero() {
        for (TraderType type : TraderType.values()) {
            assertEquals(0, registry.getCount(type), "Initial count for " + type + " should be 0");
        }
    }

    @Test
    void testSingleRegisterAndUnregister() {
        Trader mockTrader = new DummyTrader(101L, TraderType.NOISE);

        registry.register(mockTrader);
        assertEquals(1, registry.getCount(TraderType.NOISE));
        assertEquals(0, registry.getCount(TraderType.MOMENTUM));

        registry.unregister(mockTrader);
        assertEquals(0, registry.getCount(TraderType.NOISE));
    }

    @Test
    void testDuplicateRegisterDoesNotIncrement() {
        Trader mockTrader = new DummyTrader(101L, TraderType.NOISE);

        registry.register(mockTrader);
        registry.register(mockTrader); // Duplicate

        assertEquals(1, registry.getCount(TraderType.NOISE), "Duplicate registration should not increase count");
    }

    @Test
    void testDuplicateUnregisterDoesNotDecrement() {
        Trader mockTrader = new DummyTrader(101L, TraderType.NOISE);

        registry.register(mockTrader);
        assertEquals(1, registry.getCount(TraderType.NOISE));

        registry.unregister(mockTrader);
        registry.unregister(mockTrader); // Duplicate

        assertEquals(0, registry.getCount(TraderType.NOISE), "Duplicate unregistration should not decrease count below 0");
    }

    @Test
    void testUnregisterWithoutRegisterDoesNotDecrement() {
        Trader mockTrader = new DummyTrader(101L, TraderType.NOISE);

        registry.unregister(mockTrader);
        assertEquals(0, registry.getCount(TraderType.NOISE), "Unregistering non-existent trader should not go below 0");
    }

    @Test
    void testMultipleTradersSameType() {
        Trader trader1 = new DummyTrader(101L, TraderType.MOMENTUM);
        Trader trader2 = new DummyTrader(102L, TraderType.MOMENTUM);

        registry.register(trader1);
        registry.register(trader2);

        assertEquals(2, registry.getCount(TraderType.MOMENTUM));

        registry.unregister(trader1);
        assertEquals(1, registry.getCount(TraderType.MOMENTUM));

        registry.unregister(trader2);
        assertEquals(0, registry.getCount(TraderType.MOMENTUM));
    }

    @Test
    void testNullTraderHandling() {
        assertDoesNotThrow(() -> registry.register(null));
        assertDoesNotThrow(() -> registry.unregister(null));
    }

    @Test
    void testNullTraderTypeHandling() {
        Trader mockTrader = new DummyTrader(101L, null);
        assertDoesNotThrow(() -> registry.register(mockTrader));
        assertDoesNotThrow(() -> registry.unregister(mockTrader));
    }

    // Dummy implementation of Trader interface for testing
    private static class DummyTrader implements Trader {
        private final long id;
        private final TraderType type;

        public DummyTrader(long id, TraderType type) {
            this.id = id;
            this.type = type;
        }

        @Override
        public long id() {
            return id;
        }

        @Override
        public TraderType type() {
            return type;
        }

        @Override
        public List<Order> tick(Exchange market, SimulationContext context) {
            return List.of();
        }

        @Override
        public void onOrderFilled(Order order, long price, long quantity) {}
    }
}
