package ru.services;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.common.util.OutboxOperation;
import ru.core.cache.ImpulseTradesSection;
import ru.core.cache.WindowStore;
import ru.core.engine.FilterEngine;
import ru.kafka.publishers.AlertPublisher;
import ru.models.dto.AlertEvent;
import ru.models.dto.TradeEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradeProcessorIntegrationTest {

    private static final String SYM = "btcusdt";

    private TradeProcessor processor;
    private AlertPublisher alertPublisher;
    private ImpulseTradesSection section;

    @BeforeEach
    void setUp() {
        section = new ImpulseTradesSection();
        alertPublisher = mock(AlertPublisher.class);
        when(alertPublisher.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        processor = new TradeProcessor(
                new WindowStore(),
                section,
                new FilterEngine(),
                alertPublisher);
    }

    @Test
    void noFilter_noAlert() {
        processor.process(null, trade(1L, 100L));
        processor.process(null, trade(2L, 200L));
        verifyNoInteractions(alertPublisher);
    }

    @Test
    void trade_triggersOnImpulse() {
        createFilter(1L, 60, Direction.UP, 10);
        subscribe(1L, 100);

        processor.process(null, trade(1L, 100L));
        processor.process(null, trade(2L, 120L));  // 20% рост > 10%

        var record = captureRecord();
        assertEquals(Set.of(100), record.value().subscribers());
    }

    @Test
    void trade_belowThreshold_noAlert() {
        createFilter(1L, 60, Direction.UP, 10);
        subscribe(1L, 100);

        processor.process(null, trade(1L, 100L));
        processor.process(null, trade(2L, 105L));  // 5% < 10%

        verifyNoInteractions(alertPublisher);
    }

    @Test
    void trade_blacklistedSymbol_noAlert() {
        createFilterWithBlacklist(1L, 60, Direction.UP, 5, List.of(SYM));
        subscribe(1L, 100);

        int before = sendCount();
        processor.process(null, trade(1L, 100L));
        processor.process(null, trade(2L, 200L));
        assertEquals(before, sendCount());
    }

    @Test
    void trade_noSubscribe_noAlert() {
        createFilter(1L, 60, Direction.UP, 10);

        int before = sendCount();
        processor.process(null, trade(1L, 100L));
        processor.process(null, trade(2L, 200L));
        assertEquals(before, sendCount());
    }

    @Test
    void trade_multipleSubscribers_oneRecord() {
        createFilter(1L, 60, Direction.UP, 10);
        subscribe(1L, 100);
        subscribe(1L, 200);

        processor.process(null, trade(1L, 100L));
        processor.process(null, trade(2L, 120L));

        var record = captureRecord();
        assertEquals(Set.of(100, 200), record.value().subscribers());
    }

    @Test
    void trade_twoFilters_sameWindow_bothTrigger() {
        createFilter(1L, 60, Direction.UP, 10);
        createFilter(2L, 60, Direction.UP, 15);
        subscribe(1L, 100);
        subscribe(2L, 200);

        processor.process(null, trade(1L, 100L));
        processor.process(null, trade(2L, 120L));

        verify(alertPublisher, times(2)).send(any());
    }

    @Test
    void trade_twoFilters_differentWindows_onlyLowerTriggers() {
        createFilter(1L, 60, Direction.UP, 25);
        createFilter(2L, 60, Direction.UP, 10);
        subscribe(1L, 100);
        subscribe(2L, 200);

        processor.process(null, trade(1L, 100L));
        processor.process(null, trade(2L, 112L));

        var record = captureRecord();
        assertEquals(Set.of(200), record.value().subscribers());
    }

    @Test
    void trade_downMove_triggersDownFilter() {
        createFilter(1L, 60, Direction.DOWN, 15);
        subscribe(1L, 100);

        processor.process(null, trade(1L, 100L));
        processor.process(null, trade(2L, 80L));

        verify(alertPublisher, atLeastOnce()).send(any());
    }

    @Test
    void trade_downMove_noTriggerOnUpFilter() {
        createFilter(1L, 60, Direction.UP, 10);
        subscribe(1L, 100);

        int before = sendCount();
        processor.process(null, trade(1L, 100L));
        processor.process(null, trade(2L, 80L));
        assertEquals(before, sendCount(), "UP filter should not trigger on down move");
    }

    @Test
    void trade_bothDirections_upAndDown_bothTrigger() {
        createFilter(1L, 60, Direction.BOTH, 10);
        subscribe(1L, 100);

        processor.process(null, trade(1L, 100L));
        processor.process(null, trade(2L, 120L));
        assertEquals(1, captureRecords().size());

        // Сбросим — новый символ
        section = new ImpulseTradesSection();
        processor = new TradeProcessor(
                new WindowStore(), section, new FilterEngine(), alertPublisher);
        createFilter(1L, 60, Direction.BOTH, 10);
        subscribe(1L, 100);

        processor.process(null, trade("ethusdt", 1L, 100L));
        processor.process(null, trade("ethusdt", 2L, 80L));
        verify(alertPublisher, atLeast(2)).send(any());
    }

    private int sendCount() {
        return mockingDetails(alertPublisher).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("send"))
                .mapToInt(i -> 1).sum();
    }

    private void createFilter(long filterId, int timeWindow, Direction dir, int percent) {
        section.apply(event(OutboxOperation.CREATE, filterId, 1,
                impulsePayload(List.of(), timeWindow, dir, percent)));
    }

    private void createFilterWithBlacklist(long filterId, int timeWindow, Direction dir, int percent, List<String> bl) {
        section.apply(event(OutboxOperation.CREATE, filterId, 1,
                impulsePayload(bl, timeWindow, dir, percent)));
    }

    private void subscribe(long filterId, int userId) {
        section.apply(event(OutboxOperation.SUBSCRIBE, filterId, userId, null));
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

    private static OutboxCreatedEvent event(OutboxOperation op, long filterId, int userId, OutboxPayload payload) {
        return new OutboxCreatedEvent("IMPULSE", op, filterId, userId, OffsetDateTime.now(), payload);
    }

    private static OutboxPayload.ImpulseFilter impulsePayload(List<String> bl, int tw, Direction dir, int pct) {
        return new OutboxPayload.ImpulseFilter(
                List.of("binance"), List.of("futures"), bl, tw, dir, pct, 0);
    }
}
