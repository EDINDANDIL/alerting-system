package ru.services.trades;

import ru.models.dto.TradeEvent;

public interface TradesFilterProcessor {
    void process(String key, TradeEvent event);
}