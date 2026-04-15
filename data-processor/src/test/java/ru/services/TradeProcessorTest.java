package ru.services;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import ru.core.cache.Index;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.kafka.publishers.AlertPublisher;
import ru.models.dto.AlertEvent;
import ru.models.dto.TradeEvent;
import ru.models.states.ImpulseFilterView;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradeProcessorTest {

    private TradeProcessor processor;
    private Index index;
    private AlertPublisher alertPublisher;

    @BeforeEach
    void setUp() {
        index = mock(Index.class);
        alertPublisher = mock(AlertPublisher.class);
        when(alertPublisher.send(any())).thenReturn(CompletableFuture.completedFuture(null));
        processor = new TradeProcessor(index, alertPublisher);
    }

    @Nested
    class OnTrade {

        @Test
        void onTrade_callsIndexAddPoint() {
            var event = trade("btcusdt", 1L, 100L);
            processor.onTrade("key", event);
            verify(index).addPoint(event);
        }

        @Test
        void onTrade_callsIndexCheck() {
            var event = trade("btcusdt", 1L, 100L);
            processor.onTrade("key", event);
            verify(index).check("btcusdt");
        }

        @Test
        void onTrade_noTriggeredFilters_noAlert() {
            var event = trade("btcusdt", 1L, 100L);
            when(index.check("btcusdt")).thenReturn(List.of());
            processor.onTrade("key", event);
            verifyNoInteractions(alertPublisher);
        }

        @Test
        void onTrade_filterTriggeredWithSubscribers_sendsAlert() {
            var filter = filterView(1L, Direction.UP, 10, Set.of(100L));
            when(index.check("btcusdt")).thenReturn(List.of(filter));

            processor.onTrade("key", trade("btcusdt", 2L, 120L));

            verify(alertPublisher).send(any(ProducerRecord.class));
        }

        @Test
        void onTrade_filterTriggered_noSubscribers_noAlert() {
            var filter = filterView(1L, Direction.UP, 10, Set.of());
            when(index.check("btcusdt")).thenReturn(List.of(filter));

            processor.onTrade("key", trade("btcusdt", 2L, 120L));

            verifyNoInteractions(alertPublisher);
        }

        @Test
        void onTrade_multipleFilters_triggeredOnly_sendsAlert() {
            var filterWithSubs = filterView(1L, Direction.UP, 10, Set.of(100L));
            var filterWithoutSubs = filterView(2L, Direction.UP, 10, Set.of());
            when(index.check("btcusdt")).thenReturn(List.of(filterWithSubs, filterWithoutSubs));

            processor.onTrade("key", trade("btcusdt", 2L, 120L));

            verify(alertPublisher, times(1)).send(any());
        }

        @Test
        void onTrade_multipleFiltersWithSubs_sendsMultipleAlerts() {
            var filter1 = filterView(1L, Direction.UP, 10, Set.of(100L));
            var filter2 = filterView(2L, Direction.UP, 15, Set.of(200L));
            when(index.check("btcusdt")).thenReturn(List.of(filter1, filter2));

            processor.onTrade("key", trade("btcusdt", 2L, 150L));

            verify(alertPublisher, times(2)).send(any());
        }
    }

    @Nested
    class AlertContent {

        @Test
        void alert_containsCorrectSubscribers() {
            var filter = filterView(1L, Direction.UP, 10, Set.of(100L, 200L, 300L));
            when(index.check("btcusdt")).thenReturn(List.of(filter));

            processor.onTrade("key", trade("btcusdt", 2L, 120L));

            ProducerRecord<String, AlertEvent> record = captureRecord();
            assertEquals(Set.of(100L, 200L, 300L), record.value().subscribers());
        }

        @Test
        void alert_containsCorrectSymbol() {
            var filter = filterView(1L, Direction.UP, 10, Set.of(100L));
            when(index.check("ethusdt")).thenReturn(List.of(filter));

            processor.onTrade("key", trade("ethusdt", 2L, 120L));

            ProducerRecord<String, AlertEvent> record = captureRecord();
            assertEquals("ethusdt", record.value().symbol());
        }

        @Test
        void alert_containsTimestamp() {
            var filter = filterView(1L, Direction.UP, 10, Set.of(100L));
            when(index.check("btcusdt")).thenReturn(List.of(filter));
            var tradeEvent = trade("btcusdt", 2L, 120L);

            processor.onTrade("key", tradeEvent);

            ProducerRecord<String, AlertEvent> record = captureRecord();
            assertEquals(tradeEvent.timestampNs(), record.value().timestampNs());
        }

        @Test
        void alert_containsExchangeAndMarket() {
            var filter = filterView(1L, Direction.UP, 10, Set.of(100L));
            when(index.check("btcusdt")).thenReturn(List.of(filter));

            processor.onTrade("key", trade("btcusdt", 2L, 120L));

            ProducerRecord<String, AlertEvent> record = captureRecord();
            assertEquals(Set.of("binance"), record.value().exchange());
            assertEquals(Set.of("futures"), record.value().market());
        }
    }

    @Nested
    class ProducerRecordTests {

        @Test
        void producerRecord_topicIsAlertTopic() {
            var filter = filterView(1L, Direction.UP, 10, Set.of(100L));
            when(index.check("btcusdt")).thenReturn(List.of(filter));

            processor.onTrade("key", trade("btcusdt", 2L, 120L));

            ProducerRecord<String, AlertEvent> record = captureRecord();
            assertEquals("alert-topic", record.topic());
        }

        @Test
        void producerRecord_keyIsPassedThrough() {
            var filter = filterView(1L, Direction.UP, 10, Set.of(100L));
            when(index.check("btcusdt")).thenReturn(List.of(filter));

            processor.onTrade("myKey", trade("btcusdt", 2L, 120L));

            ProducerRecord<String, AlertEvent> record = captureRecord();
            assertEquals("myKey", record.key());
        }

        @Test
        void producerRecord_nullKey_handled() {
            var filter = filterView(1L, Direction.UP, 10, Set.of(100L));
            when(index.check("btcusdt")).thenReturn(List.of(filter));

            processor.onTrade(null, trade("btcusdt", 2L, 120L));

            ProducerRecord<String, AlertEvent> record = captureRecord();
            assertNull(record.key());
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void onTrade_emptyFilterList_noAlert() {
            when(index.check("btcusdt")).thenReturn(List.of());
            processor.onTrade("key", trade("btcusdt", 1L, 100L));
            verifyNoInteractions(alertPublisher);
        }
    }

    @SuppressWarnings("unchecked")
    private ProducerRecord<String, AlertEvent> captureRecord() {
        ArgumentCaptor<ProducerRecord<String, AlertEvent>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(alertPublisher).send(captor.capture());
        return captor.getValue();
    }

    private TradeEvent trade(String symbol, long id, long price) {
        return new TradeEvent("binance", "futures", symbol, id, id * 1_000_000L, 0L, 0L, price, 0L, 0L, 0, 0);
    }

    private ImpulseFilterView filterView(long filterId, Direction direction, int percent, Set<Long> subscribers) {
        var payload = new OutboxPayload.ImpulseFilter(
                Set.of("binance"), Set.of("futures"), Set.of(),
                60, direction, percent, 0);
        return new ImpulseFilterView(filterId, payload, subscribers);
    }

    private ImpulseFilterView filterViewWithNullSubscribers(long filterId) {
        var payload = new OutboxPayload.ImpulseFilter(
                Set.of("binance"), Set.of("futures"), Set.of(),
                60, Direction.UP, 10, 0);
        return new ImpulseFilterView(filterId, payload, null);
    }
}
