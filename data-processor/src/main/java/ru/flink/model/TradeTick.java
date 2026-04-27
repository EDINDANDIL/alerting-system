package ru.flink.model;


public record TradeTick(
        String symbol,
        long price,
        long timestampNs
) {}