package ru.domain.agent;

public interface AgentRegistry {
    void register(Trader trader);
    void unregister(Trader trader);
    int getCount(TraderType type);
}