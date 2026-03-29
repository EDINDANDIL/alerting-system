package ru.services.trades.window;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.core.trades.impulse.ImpulseTradesSection;
import ru.kafka.publishers.AlertPublisher;
import ru.models.dto.AlertEvent;
import ru.models.dto.TradeEvent;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ImpulseTradeWindowGraph} с использованием Mockito.
 * Демонстрирует мокирование компонентов приложения.
 */
class ImpulseTradeWindowGraphKoraTest {

    private static final String EX = "binance";
    private static final String MK = "futures";
    private static final String SYM = "btcusdt";

    @Test
    void onTrade_withActiveFilter_sendsAlert() {
        // Создаем моки
        ImpulseTradesSection section = mock(ImpulseTradesSection.class);
        AlertPublisher publisher = mock(AlertPublisher.class);
        when(publisher.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        var graph = new ImpulseTradeWindowGraph(section, publisher);

        // Создаем тестовые данные
        var payload = new OutboxPayload.ImpulseFilter(
                List.of(EX), List.of(MK), List.of(), 60, Direction.UP, 10, 0
        );
        var filterView = new ru.core.trades.impulse.ImpulseFilterView(1L, payload, Set.of(100));
        when(section.activeImpulseFilters()).thenReturn(List.of(filterView));

        // Отправляем трейды
        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 120L));

        // Проверяем, что alert был отправлен
        ArgumentCaptor<ProducerRecord<String, AlertEvent>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(publisher, atLeastOnce()).send(captor.capture());

        ProducerRecord<String, AlertEvent> record = captor.getValue();
        AlertEvent event = record.value();

        assertEquals("1:" + SYM, record.key());
        assertEquals(Set.of(100), event.subscribers());
        assertEquals(List.of(EX), event.exchange());
        assertEquals(List.of(MK), event.market());
        assertEquals(SYM, event.symbol());
    }

    @Test
    void onTrade_noActiveFilters_noAlertSent() {
        ImpulseTradesSection section = mock(ImpulseTradesSection.class);
        AlertPublisher publisher = mock(AlertPublisher.class);
        when(section.activeImpulseFilters()).thenReturn(List.of());

        var graph = new ImpulseTradeWindowGraph(section, publisher);

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 200L));

        verify(publisher, never()).send(any());
    }

    @Test
    void onTrade_blackListedSymbol_noAlertSent() {
        ImpulseTradesSection section = mock(ImpulseTradesSection.class);
        AlertPublisher publisher = mock(AlertPublisher.class);

        var payload = new OutboxPayload.ImpulseFilter(
                List.of(EX), List.of(MK), List.of(SYM), 60, Direction.UP, 5, 0
        );
        var filterView = new ru.core.trades.impulse.ImpulseFilterView(1L, payload, Set.of(50));
        when(section.activeImpulseFilters()).thenReturn(List.of(filterView));

        var graph = new ImpulseTradeWindowGraph(section, publisher);

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 200L));

        verify(publisher, never()).send(any());
    }

    @Test
    void onTrade_belowThreshold_noAlertSent() {
        ImpulseTradesSection section = mock(ImpulseTradesSection.class);
        AlertPublisher publisher = mock(AlertPublisher.class);

        var payload = new OutboxPayload.ImpulseFilter(
                List.of(EX), List.of(MK), List.of(), 60, Direction.UP, 20, 0
        );
        var filterView = new ru.core.trades.impulse.ImpulseFilterView(1L, payload, Set.of(1));
        when(section.activeImpulseFilters()).thenReturn(List.of(filterView));

        var graph = new ImpulseTradeWindowGraph(section, publisher);

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 105L)); // 5% рост

        verify(publisher, never()).send(any());
    }

    @Test
    void onTrade_downMove_triggersDownFilter() {
        ImpulseTradesSection section = mock(ImpulseTradesSection.class);
        AlertPublisher publisher = mock(AlertPublisher.class);
        when(publisher.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        var payload = new OutboxPayload.ImpulseFilter(
                List.of(EX), List.of(MK), List.of(), 60, Direction.DOWN, 15, 0
        );
        var filterView = new ru.core.trades.impulse.ImpulseFilterView(1L, payload, Set.of(1));
        when(section.activeImpulseFilters()).thenReturn(List.of(filterView));

        var graph = new ImpulseTradeWindowGraph(section, publisher);

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 80L)); // 20% падение

        ArgumentCaptor<ProducerRecord<String, AlertEvent>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(publisher).send(captor.capture());

        AlertEvent event = captor.getValue().value();
        assertEquals(Set.of(1), event.subscribers());
    }

    @Test
    void onTrade_bothDirections_upMove_triggers() {
        ImpulseTradesSection section = mock(ImpulseTradesSection.class);
        AlertPublisher publisher = mock(AlertPublisher.class);
        when(publisher.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        var payload = new OutboxPayload.ImpulseFilter(
                List.of(EX), List.of(MK), List.of(), 60, Direction.BOTH, 10, 0
        );
        var filterView = new ru.core.trades.impulse.ImpulseFilterView(1L, payload, Set.of(1));
        when(section.activeImpulseFilters()).thenReturn(List.of(filterView));

        var graph = new ImpulseTradeWindowGraph(section, publisher);

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 120L)); // 20% рост

        verify(publisher, atLeastOnce()).send(any());
    }

    @Test
    void onTrade_bothDirections_downMove_triggers() {
        ImpulseTradesSection section = mock(ImpulseTradesSection.class);
        AlertPublisher publisher = mock(AlertPublisher.class);
        when(publisher.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        var payload = new OutboxPayload.ImpulseFilter(
                List.of(EX), List.of(MK), List.of(), 60, Direction.BOTH, 10, 0
        );
        var filterView = new ru.core.trades.impulse.ImpulseFilterView(1L, payload, Set.of(1));
        when(section.activeImpulseFilters()).thenReturn(List.of(filterView));

        var graph = new ImpulseTradeWindowGraph(section, publisher);

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 80L)); // 20% падение

        verify(publisher, atLeastOnce()).send(any());
    }

    @Test
    void onTrade_multipleSubscribers_sendsMultipleAlerts() {
        ImpulseTradesSection section = mock(ImpulseTradesSection.class);
        AlertPublisher publisher = mock(AlertPublisher.class);
        when(publisher.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        var payload = new OutboxPayload.ImpulseFilter(
                List.of(EX), List.of(MK), List.of(), 60, Direction.UP, 10, 0
        );
        var filterView = new ru.core.trades.impulse.ImpulseFilterView(1L, payload, Set.of(10, 20, 30));
        when(section.activeImpulseFilters()).thenReturn(List.of(filterView));

        var graph = new ImpulseTradeWindowGraph(section, publisher);

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 120L));

        // Каждый подписчик получает отдельное сообщение
        verify(publisher, atLeastOnce()).send(any());
    }

    private TradeEvent trade(long tsNs, long priceRaw) {
        return trade(SYM, tsNs, priceRaw);
    }

    private static TradeEvent trade(String symbol, long tsNs, long priceRaw) {
        return new TradeEvent(
                EX,
                MK,
                symbol,
                1L,
                tsNs,
                0L,
                0L,
                priceRaw,
                0L,
                0L,
                0,
                0
        );
    }
}
