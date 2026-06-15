package ru.domain.simulation;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.config.SimulatorConfig;
import ru.domain.agent.AgentRegistry;
import ru.domain.agent.TraderType;
import ru.domain.market.Exchange;
import ru.publishers.TradePublisher;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SimulationEngineTest {

    @Test
    void testSimulationLifecycleAndAgentRegistry() throws Exception {
        TradePublisher mockPublisher = mock(TradePublisher.class);
        Exchange exchange = new Exchange(mockPublisher);

        // Setup config with specific agent counts
        int noiseCount = 3;
        int momentumCount = 2;
        int fundamentalCount = 1;
        int marketMakerCount = 2;

        SimulatorConfig config = new SimulatorConfig(
                5L,                            // tickDelayMs
                10L,                           // ticks (run 10 ticks and then stop)
                List.of("BTC"),               // symbols
                Map.of("BTC", 100L),          // startPrices
                Map.of("BTC", 1000L),         // tickSizes
                Map.of("BTC", 1.0),           // targetUsdVolumes
                noiseCount,
                momentumCount,
                fundamentalCount,
                marketMakerCount
        );

        SimulationEngine engine = new SimulationEngine(config, exchange);

        // Retrieve the private agentRegistry field via reflection
        Field registryField = SimulationEngine.class.getDeclaredField("agentRegistry");
        registryField.setAccessible(true);
        AgentRegistry registry = (AgentRegistry) registryField.get(engine);

        assertNotNull(registry, "Agent registry should be initialized");

        // Before start: all agent counts should be 0
        assertEquals(0, registry.getCount(TraderType.NOISE));
        assertEquals(0, registry.getCount(TraderType.MOMENTUM));
        assertEquals(0, registry.getCount(TraderType.FUNDAMENTAL));
        assertEquals(0, registry.getCount(TraderType.MARKET_MAKER));

        // Start the engine
        engine.start();
        assertTrue(engine.isRunning(), "Engine should be running");

        // After start: agent counts must match config exactly
        assertEquals(noiseCount, registry.getCount(TraderType.NOISE));
        assertEquals(momentumCount, registry.getCount(TraderType.MOMENTUM));
        assertEquals(fundamentalCount, registry.getCount(TraderType.FUNDAMENTAL));
        assertEquals(marketMakerCount, registry.getCount(TraderType.MARKET_MAKER));

        // Wait for it to complete the ticks
        long start = System.currentTimeMillis();
        while (engine.isRunning() && (System.currentTimeMillis() - start) < 1000) {
            Thread.sleep(10);
        }

        // Stop the engine to trigger unregistration
        engine.stop();
        assertFalse(engine.isRunning(), "Engine should be stopped");

        // After stop: agent counts must return to 0 (all unregistered)
        assertEquals(0, registry.getCount(TraderType.NOISE));
        assertEquals(0, registry.getCount(TraderType.MOMENTUM));
        assertEquals(0, registry.getCount(TraderType.FUNDAMENTAL));
        assertEquals(0, registry.getCount(TraderType.MARKET_MAKER));

        engine.release();
    }
}
