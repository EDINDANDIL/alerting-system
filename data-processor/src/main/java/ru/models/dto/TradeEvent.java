package ru.models.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

@Json
public record TradeEvent(
        String exchange,      // "binance"
        String market,        // "futures"
        String symbol,        // "btcusdt"
        long id,              // trade id
        long timestampNs,     // как в C++
        long firstTradeId,
        long lastTradeId,
        long price,           // long, как raw в C++
        long amount,          // long, как raw
        long quoteQty,        // long, как raw
        int side,             // byte -> int
        int isBuyerMaker      // byte -> int
) {}
