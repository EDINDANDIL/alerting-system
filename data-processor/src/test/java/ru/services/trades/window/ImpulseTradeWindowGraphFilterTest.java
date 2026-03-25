package ru.services.trades.window;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.common.util.OutboxOperation;
import ru.core.trades.impulse.ImpulseTradesSection;
import ru.kafka.publishers.AlertPublisher;
import ru.models.dto.AlertEvent;
import ru.models.dto.TradeEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImpulseTradeWindowGraphFilterTest {

    private static final String EX = "binance";
    private static final String MK = "futures";
    private static final String SYM = "btcusdt";

    private ImpulseTradesSection section;
    private AlertPublisher alerts;
    private ImpulseTradeWindowGraph graph;

    @BeforeEach
    void setUp() {
        section = new ImpulseTradesSection();
        alerts = mock(AlertPublisher.class);

        // Важно: send(...).whenComplete(...) в коде graph не должен падать на null
        when(alerts.send(any())).thenReturn(CompletableFuture.completedFuture(null));

        graph = new ImpulseTradeWindowGraph(section, alerts);
    }

    @Test
    void upMove_aboveThreshold_triggersOnce() {
        register(1L, 100, impulse(List.of(), 60, Direction.UP, 10));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 120L));

        var records = captureSentRecords();
        var record = records.getFirst();

        assertEquals(SYM, record.key());

        AlertEvent sent = record.value();
        assertEquals(1L, sent.filterId());
        assertEquals(100, sent.userId());
        var impulsePayload = assertInstanceOf(OutboxPayload.ImpulseFilter.class, sent.payload());
        assertEquals(10, impulsePayload.percent());
    }

    @Test
    void upMove_belowThreshold_noTrigger() {
        register(1L, 1, impulse(List.of(), 60, Direction.UP, 10));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 105L));

        verifyNoInteractions(alerts);
    }

    @Test
    void upMove_exactlyAtThreshold_triggers() {
        register(1L, 1, impulse(List.of(), 60, Direction.UP, 10));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 110L));

        verify(alerts, times(1)).send(any());
    }

    @Test
    void downMove_magnitudeMeetsThreshold_triggers() {
        register(1L, 1, impulse(List.of(), 60, Direction.DOWN, 10));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 80L));

        verify(alerts, times(1)).send(any());
    }

    @Test
    void bothDirection_negativeDelta_triggersOnAbs() {
        register(1L, 1, impulse(List.of(), 60, Direction.BOTH, 10));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 80L));

        verify(alerts, times(1)).send(any());
    }

    @Test
    void upFilter_onDownMove_noTrigger() {
        register(1L, 1, impulse(List.of(), 60, Direction.UP, 10));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 80L));

        verifyNoInteractions(alerts);
    }

    @Test
    void blackListedSymbol_noTrigger() {
        register(1L, 1, impulse(List.of(SYM), 60, Direction.UP, 5));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 200L));

        verifyNoInteractions(alerts);
    }

    @Test
    void exchangeMismatch_noTrigger() {
        register(1L, 1, impulse(List.of("bybit"), List.of(MK), List.of(), 60, Direction.UP, 5));

        graph.onTrade(trade(SYM, 1_000L, 100L));
        graph.onTrade(trade(SYM, 2_000L, 200L));

        verifyNoInteractions(alerts);
    }

    @Test
    void marketMismatch_noTrigger() {
        register(1L, 1, impulse(List.of(EX), List.of("spot"), List.of(), 60, Direction.UP, 5));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 200L));

        verifyNoInteractions(alerts);
    }

    @Test
    void createWithoutSubscribe_noActiveFilters_noTrigger() {
        section.apply(event(OutboxOperation.CREATE, 1L, 1,
                impulse(List.of(), 60, Direction.UP, 5)));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 200L));

        verifyNoInteractions(alerts);
    }

    @Test
    void twoSubscribers_twoAlertsSameFilter() {
        registerTwoUsers(impulse(List.of(), 60, Direction.UP, 10));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 120L));

        verify(alerts, times(2)).send(any());
    }

    @Test
    void twoFiltersDifferentPercent_onlyLowerThresholdFires() {
        register(1L, 1, impulse(List.of(), 60, Direction.UP, 25));
        register(2L, 1, impulse(List.of(), 60, Direction.UP, 10));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 112L));

        var records = captureSentRecords();
        var sent = records.getFirst().value();

        assertEquals(2L, sent.filterId());
        var impulsePayload = assertInstanceOf(OutboxPayload.ImpulseFilter.class, sent.payload());
        assertEquals(10, impulsePayload.percent());
    }

    @Test
    void twoFiltersSameWindowShareState_bothFireOnLargeMove() {
        register(1L, 1, impulse(List.of(), 60, Direction.UP, 10));
        register(2L, 1, impulse(List.of(), 60, Direction.UP, 15));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 120L));

        verify(alerts, times(2)).send(any());
    }

    @Test
    void differentTimeWindows_independentImpulse() {
        register(1L, 1, impulse(List.of(), 10, Direction.UP, 10));
        register(2L, 1, impulse(List.of(), 60, Direction.UP, 10));

        graph.onTrade(trade(1_000L, 100L));
        graph.onTrade(trade(2_000L, 120L));

        verify(alerts, times(2)).send(any());
    }

    @Test
    void otherSymbol_notBlackListed_triggersWithSameFilterRule() {
        register(1L, 1, impulse(List.of(), 60, Direction.UP, 5));

        graph.onTrade(trade("ethusdt", 1_000L, 100L));
        graph.onTrade(trade("ethusdt", 2_000L, 200L));

        var records = captureSentRecords();
        assertEquals("ethusdt", records.getFirst().key());
    }

    @Test
    void otherSymbol_blackListed_noTrigger() {
        register(1L, 1, impulse(List.of("ethusdt"), 60, Direction.UP, 5));

        graph.onTrade(trade("ethusdt", 1_000L, 100L));
        graph.onTrade(trade("ethusdt", 2_000L, 200L));

        verifyNoInteractions(alerts);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<ProducerRecord<String, AlertEvent>> captureSentRecords() {
        ArgumentCaptor<ProducerRecord<String, AlertEvent>> cap =
                ArgumentCaptor.forClass((Class) ProducerRecord.class);
        verify(alerts, times(1)).send(cap.capture());
        return cap.getAllValues();
    }

    private void register(long filterId, int userId, OutboxPayload.ImpulseFilter payload) {
        section.apply(event(OutboxOperation.CREATE, filterId, userId, payload));
        section.apply(event(OutboxOperation.SUBSCRIBE, filterId, userId, null));
    }

    private void registerTwoUsers(OutboxPayload.ImpulseFilter payload) {
        section.apply(event(OutboxOperation.CREATE, 1L, 10, payload));
        section.apply(event(OutboxOperation.SUBSCRIBE, 1L, 10, null));
        section.apply(event(OutboxOperation.SUBSCRIBE, 1L, 20, null));
    }

    private static OutboxCreatedEvent event(
            OutboxOperation op, long filterId, int userId, OutboxPayload payload
    ) {
        return new OutboxCreatedEvent(
                "IMPULSE", op, filterId, userId, OffsetDateTime.now(), payload
        );
    }

    private static OutboxPayload.ImpulseFilter impulse(
            List<String> exchange,
            List<String> market,
            List<String> blackList,
            int timeWindowSec,
            Direction direction,
            int percent
    ) {
        return new OutboxPayload.ImpulseFilter(
                exchange, market, blackList, timeWindowSec, direction, percent, 0
        );
    }

    private static OutboxPayload.ImpulseFilter impulse(
            List<String> blackList,
            int timeWindowSec,
            Direction direction,
            int percent
    ) {
        return impulse(List.of(EX), List.of(MK), blackList, timeWindowSec, direction, percent);
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