package ru.services.trades.window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.core.trades.TradePoint;
import ru.core.trades.impulse.ImpulseFilterView;
import ru.core.trades.impulse.ImpulseTradesSection;
import ru.kafka.publishers.AlertPublisher;
import ru.models.dto.AlertEvent;
import ru.models.dto.TradeEvent;
import ru.tinkoff.kora.common.Component;
import ru.util.SlidingWindow;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Валюта (symbol) → окна по длине L (нс) → {@link SlidingWindow}.
 * Фильтры с одинаковым {@code timeWindow} делят одно окно на символ.
 */
@Component
public final class ImpulseTradeWindowGraph {

    private static final Logger log = LoggerFactory.getLogger(ImpulseTradeWindowGraph.class);

    private final ImpulseTradesSection impulseTradesSection;
    private final AlertPublisher alertPublisher;

    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, SlidingWindow>> bySymbol =
            new ConcurrentHashMap<>();

    public ImpulseTradeWindowGraph(ImpulseTradesSection impulseTradesSection, AlertPublisher alertPublisher) {
        this.impulseTradesSection = impulseTradesSection;
        this.alertPublisher = alertPublisher;
    }

    public void onTrade(TradeEvent event) {
        List<ImpulseFilterView> active = impulseTradesSection.activeImpulseFilters();
        if (active.isEmpty()) return;

        var matching = active.stream()
                .filter(f -> matchesInstrument(f, event))
                .toList();
        if (matching.isEmpty()) return;

        var point = new TradePoint(event.timestampNs(), event.price());
        var byTimeWindowSec = matching.stream()
                .collect(Collectors.groupingBy(f -> f.payload().timeWindow()));

        for (var e : byTimeWindowSec.entrySet()) {
            int timeWindowSec = e.getKey();
            long windowLengthNs = timeWindowSec * 1_000_000_000L;
            SlidingWindow window = windowFor(event.symbol(), windowLengthNs);

            double phiPercent;
            synchronized (window) {
                window.add(point);
                phiPercent = window.getCurrentImpulsePercent() * 100.0;
            }

            for (ImpulseFilterView filter : e.getValue()) {
                if (phiPercent < filter.payload().percent()) {
                    continue;
                }
                // TODO: Direction UP/DOWN/BOTH по динамике цены
                for (Integer userId : filter.subscribers()) {
                    alertPublisher.sendAsync(
                            event.symbol(),
                            new AlertEvent("IMPULSE filter=" + filter.filterId()
                                           + " user=" + userId
                                           + " symbol=" + event.symbol()
                                           + " phi=" + String.format("%.4f", phiPercent) + "%")
                    );

                    log.info("Обработали {}", userId);
                }
            }
        }
    }

    private SlidingWindow windowFor(String symbol, long windowLengthNs) {
        return bySymbol
                .computeIfAbsent(symbol, s -> new ConcurrentHashMap<>())
                .computeIfAbsent(windowLengthNs, SlidingWindow::new);
    }

    private static boolean matchesInstrument(ImpulseFilterView f, TradeEvent e) {
        var p = f.payload();
        if (p.blackList() != null && p.blackList().contains(e.symbol())) {
            return false;
        }
        if (p.exchange() != null && p.exchange().stream().noneMatch(ex -> ex.equalsIgnoreCase(e.exchange()))) {
            return false;
        }
        return p.market() == null || p.market().stream().anyMatch(m -> m.equalsIgnoreCase(e.market()));
    }
}