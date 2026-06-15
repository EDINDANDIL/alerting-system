package ru.domain.simulation;

import ru.domain.agent.AgentRegistry;

public interface SimulationContext {
    long getCurrentTick();
    AgentRegistry getRegistry();
    long getFundamentalPrice(String symbol);
}
