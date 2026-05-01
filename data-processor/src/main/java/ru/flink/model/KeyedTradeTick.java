package ru.flink.model;

public record KeyedTradeTick(
        String symbol,
        long price,
        long timestampNs
) {}