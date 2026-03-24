package ru.services.trades;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.models.dto.TradeEvent;
import ru.tinkoff.kora.application.graph.All;
import ru.tinkoff.kora.common.Component;

@Component
public final class TradesProcessor {

    private final All<TradesFilterProcessor> processors;
    private final Logger log = LoggerFactory.getLogger(TradesProcessor.class);

    public TradesProcessor(All<TradesFilterProcessor> processors) {
        this.processors = processors;
    }

    public void process(String key, TradeEvent event) {
        for (TradesFilterProcessor processor : processors) {
            log.info("Выполнил работу {}", processor.getClass());
            processor.process(key, event);
        }
    }
}
