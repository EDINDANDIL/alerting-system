package ru.services.trades.impulse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.core.trades.impulse.ImpulseTradesSection;
import ru.services.trades.window.ImpulseTradeWindowGraph;
import ru.kafka.publishers.AlertPublisher;
import ru.models.dto.TradeEvent;
import ru.services.trades.TradesFilterProcessor;
import ru.tinkoff.kora.common.Component;

@Component
public final class ImpulseTradeFilterProcessor implements TradesFilterProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImpulseTradeFilterProcessor.class);
    private final ImpulseTradeWindowGraph tradeWindowGraph;

    public ImpulseTradeFilterProcessor(

            ImpulseTradesSection impulseTradesSection,
            AlertPublisher alertPublisher, ImpulseTradeWindowGraph tradeWindowGraph

    ) {this.tradeWindowGraph = tradeWindowGraph;}

    @Override
    public void process(String key, TradeEvent event) {
        tradeWindowGraph.onTrade(event);
    }
}