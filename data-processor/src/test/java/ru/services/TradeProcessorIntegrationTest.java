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
        windowStore.getOrCompute(SYM, 60_000_000_000L);
    }

    @Test
    void noFilter_noAlert() {
        processor.onTrade(null, trade(1L, 100L));
        processor.onTrade(null, trade(2L, 200L));
        verifyNoInteractions(alertPublisher);
    }

    @Test
    void trade_belowThreshold_noAlert() {
        createFilter();
        subscribe();

        processor.onTrade(null, trade(1L, 100L));
        processor.onTrade(null, trade(2L, 105L));

        verifyNoInteractions(alertPublisher);
    }

    @Test
    void trade_blacklistedSymbol_noAlert() {
        createFilterWithBlacklist(List.of(SYM));
        subscribe();

        processor.onTrade(null, trade(1L, 100L));
        processor.onTrade(null, trade(2L, 200L));

        verifyNoInteractions(alertPublisher);
    }

    @Test
    void trade_noSubscribe_noAlert() {
        createFilter();

        processor.onTrade(null, trade(1L, 100L));
        processor.onTrade(null, trade(2L, 200L));

        verifyNoInteractions(alertPublisher);
    }

    @Test
    void trade_downMove_noTriggerOnUpFilter() {
        createFilter();
        subscribe();

        processor.onTrade(null, trade(1L, 100L));
        processor.onTrade(null, trade(2L, 80L));

        verifyNoInteractions(alertPublisher);
    }

    private void createFilter() {
        var payload = impulsePayload(Set.of(), 60, Direction.UP, 10);
        filterStore.put(1L, new ImpulseFilterView(
                1L, payload, new HashSet<>()));
        index.create(1L, null, 60);
        windowStore.getOrCompute(SYM, 60 * 1_000_000_000L);
    }

    private void createFilterWithBlacklist(List<String> bl) {
        var payload = impulsePayload(new HashSet<>(bl), 60, Direction.UP, 5);
        filterStore.put(1L, new ImpulseFilterView(
                1L, payload, new HashSet<>()));
        index.create(1L, Set.copyOf(bl), 60);
        windowStore.getOrCompute(SYM, 60 * 1_000_000_000L);
    }

    private void subscribe() {
        var old = filterStore.get(1L);
        if (old != null) {
            Set<Long> newSubs = new HashSet<>(old.subscribers());
            newSubs.add((long) 100);
            filterStore.put(1L, new ImpulseFilterView(
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
