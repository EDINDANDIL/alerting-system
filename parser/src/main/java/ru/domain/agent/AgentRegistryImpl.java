package ru.domain.agent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AgentRegistryImpl implements AgentRegistry {
    private final Set<Long> registeredTraderIds = ConcurrentHashMap.newKeySet();
    private final Map<TraderType, Integer> counts = new ConcurrentHashMap<>();

    public AgentRegistryImpl() {
        for (TraderType type : TraderType.values()) {
            counts.put(type, 0);
        }
    }

    @Override
    public synchronized void register(Trader trader) {
        if (trader == null || trader.type() == null) return;
        if (registeredTraderIds.add(trader.id())) {
            counts.merge(trader.type(), 1, Integer::sum);
        }
    }

    @Override
    public synchronized void unregister(Trader trader) {
        if (trader == null || trader.type() == null) return;
        if (registeredTraderIds.remove(trader.id())) {
            counts.merge(trader.type(), -1, (oldVal, newVal) -> Math.max(0, oldVal + newVal));
        }
    }

    @Override
    public int getCount(TraderType type) {
        return counts.getOrDefault(type, 0);
    }
}
