package ru.services.trades.impulse;


import ru.services.trades.window.ImpulseTradeWindowGraph;
import ru.models.dto.TradeEvent;
import ru.services.trades.TradesFilterProcessor;
import ru.tinkoff.kora.common.Component;

@Component
public final class ImpulseTradeFilterProcessor implements TradesFilterProcessor {

    private final ImpulseTradeWindowGraph tradeWindowGraph;

    public ImpulseTradeFilterProcessor(ImpulseTradeWindowGraph tradeWindowGraph) {
        this.tradeWindowGraph = tradeWindowGraph;
    }

    @Override
    public void process(String key, TradeEvent event) {
        tradeWindowGraph.onTrade(event);
    }
}