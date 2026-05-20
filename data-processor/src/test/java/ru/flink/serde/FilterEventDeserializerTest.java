package ru.flink.serde;

import org.junit.jupiter.api.Test;
import ru.common.dto.FilterCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.common.mappers.serde.FilterCreatedEventSerializer;
import ru.common.util.Direction;
import ru.common.util.OutboxOperation;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FilterEventDeserializerTest {

    private final FilterCreatedEventSerializer serializer = new FilterCreatedEventSerializer();
    private final FilterEventDeserializer deserializer = new FilterEventDeserializer();

    //TODO посмотреть тесты
    @Test
    void deserialize_createEventWithImpulsePayload_restoresEvent() throws Exception {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-20T10:15:30Z");
        OutboxPayload.ImpulseFilter payload = new OutboxPayload.ImpulseFilter(
                Set.of("BINANCE", "BYBIT"),
                Set.of("FUTURES"),
                Set.of("TON", "BNB"),
                60L,
                Direction.UP,
                10L,
                1_000_000L
        );
        FilterCreatedEvent event = new FilterCreatedEvent(
                "IMPULSE",
                OutboxOperation.CREATE,
                11L,
                22L,
                createdAt,
                payload
        );

        FilterCreatedEvent actual = deserializer.deserialize(serializer.serialize("filter-topic", event));

        assertNotNull(actual);
        assertEquals(event.action(), actual.action());
        assertEquals(event.operation(), actual.operation());
        assertEquals(event.filterId(), actual.filterId());
        assertEquals(event.userId(), actual.userId());
        assertEquals(event.createdAt().toInstant(), actual.createdAt().toInstant());
        assertInstanceOf(OutboxPayload.ImpulseFilter.class, actual.payload());

        OutboxPayload.ImpulseFilter actualPayload = (OutboxPayload.ImpulseFilter) actual.payload();
        assertEquals(payload.exchange(), actualPayload.exchange());
        assertEquals(payload.market(), actualPayload.market());
        assertEquals(payload.blackList(), actualPayload.blackList());
        assertEquals(payload.timeWindow(), actualPayload.timeWindow());
        assertEquals(payload.direction(), actualPayload.direction());
        assertEquals(payload.percent(), actualPayload.percent());
        assertEquals(payload.volume24h(), actualPayload.volume24h());
    }

    //TODO посмотреть тесты
    @Test
    void deserialize_deleteEventWithNullPayload_restoresNullPayload() throws Exception {
        FilterCreatedEvent event = new FilterCreatedEvent(
                "IMPULSE",
                OutboxOperation.DELETE,
                11L,
                22L,
                OffsetDateTime.parse("2026-05-20T10:15:30Z"),
                null
        );

        FilterCreatedEvent actual = deserializer.deserialize(serializer.serialize("filter-topic", event));

        assertNotNull(actual);
        assertEquals(event.action(), actual.action());
        assertEquals(event.operation(), actual.operation());
        assertEquals(event.filterId(), actual.filterId());
        assertEquals(event.userId(), actual.userId());
        assertNull(actual.payload());
    }

    //TODO посмотреть тесты
    @Test
    void deserialize_nullMessage_returnsNull() throws Exception {
        assertNull(deserializer.deserialize(null));
    }

    //TODO посмотреть тесты
    @Test
    void isEndOfStream_alwaysFalse() {
        assertFalse(deserializer.isEndOfStream(null));
    }

    //TODO посмотреть тесты
    @Test
    void getProducedType_returnsOutboxCreatedEventType() {
        assertEquals(FilterCreatedEvent.class, deserializer.getProducedType().getTypeClass());
    }
}
