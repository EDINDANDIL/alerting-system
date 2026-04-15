package ru.services;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.core.cache.FilterStore;
import ru.core.cache.Index;
import ru.core.engine.FilterEngine;
import ru.core.util.MonetStore;
import ru.core.cache.WindowStore;
import ru.kafka.publishers.AlertPublisher;
import ru.models.dto.AlertEvent;
import ru.models.dto.TradeEvent;
import ru.models.states.ImpulseFilterView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradeProcessorIntegrationTest {

    private static final String SYM = "ETH";
    private static final String SYM2 = "BST";

    private TradeProcessor processor;
    private AlertPublisher alertPublisher;
    private Index index;
    private FilterStore filterStore;
    private WindowStore windowStore;

    @BeforeEach
    void setUp() {
        filterStore = new FilterStore();
        windowStore = new WindowStore();
        FilterEngine filterEngine = new FilterEngine();
        MonetStore monetStore = new MonetStore();

        index = new Index(monetStore, windowStore, filterStore, filterEngine);
        alertPublisher = mock(AlertPublisher.class);
        when(alertPublisher.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        processor = new TradeProcessor(index, alertPublisher);
        windowStore.getOrCompute(60_000_000_000L);
    }

    @Test
    void noFilter_noAlert() {
        processor.onTrade(null, trade(1L, 100L));
        processor.onTrade(null, trade(2L, 200L));
        verifyNoInteractions(alertPublisher);
    }

    @Test
    void trade_belowThreshold_noAlert() {
        createFilter(1L, 60, Direction.UP, 10);
        subscribe(1L, 100);

        processor.onTrade(null, trade(1L, 100L));
        processor.onTrade(null, trade(2L, 105L));

        verifyNoInteractions(alertPublisher);
    }

    @Test
    void trade_blacklistedSymbol_noAlert() {
        createFilterWithBlacklist(1L, 60, Direction.UP, 5, List.of(SYM));
        subscribe(1L, 100);

        processor.onTrade(null, trade(1L, 100L));
        processor.onTrade(null, trade(2L, 200L));

        verifyNoInteractions(alertPublisher);
    }

    @Test
    void trade_noSubscribe_noAlert() {
        createFilter(1L, 60, Direction.UP, 10);

        processor.onTrade(null, trade(1L, 100L));
        processor.onTrade(null, trade(2L, 200L));

        verifyNoInteractions(alertPublisher);
    }

    @Test
    void trade_downMove_noTriggerOnUpFilter() {
        createFilter(1L, 60, Direction.UP, 10);
        subscribe(1L, 100);

        processor.onTrade(null, trade(1L, 100L));
        processor.onTrade(null, trade(2L, 80L));

        verifyNoInteractions(alertPublisher);
    }

    private void createFilter(long filterId, int timeWindow, Direction dir, int percent) {
        var payload = impulsePayload(Set.of(), timeWindow, dir, percent);
        filterStore.put(filterId, new ImpulseFilterView(
            filterId, payload, new HashSet<>()));
        index.create(filterId, null, timeWindow);
        windowStore.getOrCompute(timeWindow * 1_000_000_000L);
    }

    private void createFilterWithBlacklist(long filterId, int timeWindow, Direction dir, int percent, List<String> bl) {
        var payload = impulsePayload(new HashSet<>(bl), timeWindow, dir, percent);
        filterStore.put(filterId, new ImpulseFilterView(
            filterId, payload, new HashSet<>()));
        index.create(filterId, Set.copyOf(bl), timeWindow);
        windowStore.getOrCompute(timeWindow * 1_000_000_000L);
    }

    private void subscribe(long filterId, int userId) {
        var old = filterStore.get(filterId);
        if (old != null) {
            Set<Long> newSubs = new HashSet<>(old.subscribers());
            newSubs.add((long) userId);
            filterStore.put(filterId, new ImpulseFilterView(
                old.filterId(), old.payload(), newSubs));
        }
    }

    @SuppressWarnings("unchecked")
    private ProducerRecord<String, AlertEvent> captureRecord() {
        var captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(alertPublisher).send(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<ProducerRecord<String, AlertEvent>> captureRecords() {
        var captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(alertPublisher, atLeastOnce()).send(captor.capture());
        return (List) captor.getAllValues();
    }

    private static TradeEvent trade(long id, long price) {
        return trade(SYM, id, price);
    }

    private static TradeEvent trade(String symbol, long id, long price) {
        return new TradeEvent("binance", "futures", symbol, id, id * 1_000_000L, 0L, 0L, price, 0L, 0L, 0, 0);
    }

    private static OutboxPayload.ImpulseFilter impulsePayload(Set<String> bl, int tw, Direction dir, int pct) {
        return new OutboxPayload.ImpulseFilter(
                Set.of("binance"), Set.of("futures"), bl, tw, dir, pct, 0);
    }
}
