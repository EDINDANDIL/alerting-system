package ru.alertcli.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO для алерта от alert-service.
 * Формат: {"subscribers":[1],"exchange":["binance"],"market":["futures"],"symbol":"BTCUSDC","timestampNs":1234567890}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertMessage(
        List<String> subscribers,
        List<String> exchange,
        List<String> market,
        String symbol,
        long timestampNs
) {
    public String getExchange() {
        return exchange != null && !exchange.isEmpty() ? exchange.get(0) : "UNKNOWN";
    }

    public String getMarket() {
        return market != null && !market.isEmpty() ? market.get(0) : "UNKNOWN";
    }

    public String getSymbol() {
        return symbol != null ? symbol : "UNKNOWN";
    }

    public long getTimestampMs() {
        return timestampNs / 1_000_000;
    }

    @Override
    public String toString() {
        return String.format("%s | %s %s | %s",
                symbol,
                getExchange(),
                getMarket(),
                new java.util.Date(getTimestampMs()));
    }
}
