package ru.services;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.models.domain.TradePoint;
import ru.models.states.ImpulseFilterView;
import ru.core.cache.ImpulseTradesSection;
import ru.core.engine.FilterEngine;
import ru.core.cache.WindowStore;
import ru.kafka.publishers.AlertPublisher;
import ru.models.dto.AlertEvent;
import ru.models.dto.TradeEvent;
import ru.core.util.SlidingWindow;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.tinkoff.kora.common.Component;

/**
 * Главный entry point для обработки трейдов.
 * Координирует: обновить окна → проверить фильтры → отправить алерты.
 * НЕ хранит данные — только оркестрирует.
 */
@Component
public final class TradeProcessor {

    private static final Logger log = LoggerFactory.getLogger(TradeProcessor.class);

    private final WindowStore windowStore;
    private final ImpulseTradesSection impulseTradesSection;
    private final FilterEngine filterEngine;
    private final AlertPublisher alertPublisher;

    public TradeProcessor(
            WindowStore windowStore,
            ImpulseTradesSection impulseTradesSection,
            FilterEngine filterEngine,
            AlertPublisher alertPublisher) {

        this.windowStore = windowStore;
        this.impulseTradesSection = impulseTradesSection;
        this.filterEngine = filterEngine;
        this.alertPublisher = alertPublisher;
    }

    public void process(String key, TradeEvent event) {

        String symbol = event.symbol();
        long ts = event.timestampNs();
        long price = event.price();

        Map<Long, Set<ImpulseFilterView>> filtersByWindow = impulseTradesSection.filtersByWindow();
        if (filtersByWindow.isEmpty()) return;

        if (!impulseTradesSection.hasAnySubscriber()) return;

        for (long twNs : filtersByWindow.keySet()) {
            windowStore.getOrComputeWindow(symbol, twNs).add(new TradePoint(ts, price));
        }

        Map<Long, SlidingWindow> windows = windowStore.getWindows(symbol);
        if (windows == null || windows.isEmpty()) return;

        Set<Long> blacklistedIds = impulseTradesSection.blacklistFor(symbol);

        List<ImpulseFilterView> triggered = filterEngine.checkAll(
                symbol, windows, filtersByWindow, blacklistedIds);

        for (ImpulseFilterView filter : triggered) {
            if (filter.subscribers().isEmpty()) continue;

            String k = filter.filterId() + ":" + symbol;
            AlertEvent alertEvent = new AlertEvent(
                    filter.subscribers(),
                    filter.payload().exchange(),
                    filter.payload().market(),
                    symbol,
                    ts
            );

            // TODO другой лог
            alertPublisher.send(new ProducerRecord<>("alert-topic", k, alertEvent))
                    .whenComplete((meta, ex) -> {
                        if (ex != null) {
                            log.error("ALERT send failed: filterId={}", filter.filterId(), ex);
                        } else {
                            log.debug("Alert sent: partition={} offset={}", meta.partition(), meta.offset());
                            log.info("Я отправил {}", alertEvent);
                        }
                    });
        }
    }
}