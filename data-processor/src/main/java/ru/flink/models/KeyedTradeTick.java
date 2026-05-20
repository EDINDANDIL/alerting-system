package ru.flink.models;

public record KeyedTradeTick(
        String symbol,
        long price,
        long timestampNs
) {}