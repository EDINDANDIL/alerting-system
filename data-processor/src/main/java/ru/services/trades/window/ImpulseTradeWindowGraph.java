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

    // {монета:{окно: очередь событий}}
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, SlidingWindow>> bySymbol =
            new ConcurrentHashMap<>();

    public ImpulseTradeWindowGraph(ImpulseTradesSection impulseTradesSection, AlertPublisher alertPublisher) {
        this.impulseTradesSection = impulseTradesSection;
        this.alertPublisher = alertPublisher;
    }

    public void onTrade(TradeEvent event) {

        List<ImpulseFilterView> active = impulseTradesSection.activeImpulseFilters();

        if (active.isEmpty()) return;

        var table = active.stream()
                .filter(f -> matchesInstrument(f, event))
                .collect(Collectors.groupingBy(f -> f.payload().timeWindow()));

        if (table.isEmpty()) return;

        var point = new TradePoint(event.timestampNs(), event.price());

        table.forEach((time, filters) -> {

            SlidingWindow window = windowFor(event.symbol(), time * 1_000_000_000L);
            double delta;

            window.add(point);
            delta = window.getCurrentImpulsePercent();

            filters.stream().filter(f -> trigger(delta, f))
                    .forEach(f -> {
                        f.subscribers().forEach(id -> {
                            alertPublisher.sendAsync(
                                    event.symbol(),
                                    new AlertEvent(f.filterId(), id, f.payload())
                            );
                            log.info("Обработали {}", id);
                        });
                    });
        });
    }

    // Вызов конкретного окна с конкретным значением времени
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

    private boolean trigger(double delta, ImpulseFilterView filter) {

        double threshold = filter.payload().percent() / 100.0;
        return switch (filter.payload().direction()) {
            case UP -> delta >= threshold; // TODO percent -> coef
            case DOWN -> delta <= -threshold;
            case BOTH ->  Math.abs(delta) >= threshold;
        };
    }
}