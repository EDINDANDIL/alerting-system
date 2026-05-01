package ru.dto;

public record TradeTick(
        long price, //real price = price / (10 ** 8)
        long timestampNs
) {}
