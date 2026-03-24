package ru.core.trades.impulse;

import org.junit.jupiter.api.Test;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.common.util.OutboxOperation;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// тест на правильное поведение хороших случаев
class ImpulseTradesSectionTest {

    @Test
    void CRUD_actions() {
        var section = new ImpulseTradesSection();

        long filterId = 1;
        int userId = 1;

        OutboxPayload.ImpulseFilter payload = new OutboxPayload.ImpulseFilter(
                List.of("binance"),
                List.of("futures"),
                List.of(),
                50,
                Direction.UP,
                5,
                0
        );

        section.apply(new OutboxCreatedEvent(
                "IMPULSE",
                OutboxOperation.CREATE,
                filterId,
                userId,
                OffsetDateTime.now(),
                payload
        ));

        assertTrue(section.activeImpulseFilters().isEmpty(), "Create alone should not make filter active");

        section.apply(new OutboxCreatedEvent(
                "IMPULSE",
                OutboxOperation.SUBSCRIBE,
                filterId,
                userId,
                OffsetDateTime.now(),
                null
        ));

        var active = section.activeImpulseFilters();
        assertEquals(1, active.size());
        assertEquals(Direction.UP, active.getFirst().payload().direction());
        assertTrue(active.getFirst().subscribers().contains(userId));

        section.apply(new OutboxCreatedEvent(
                "IMPULSE",
                OutboxOperation.UNSUBSCRIBE,
                filterId,
                userId,
                OffsetDateTime.now(),
                null
        ));

        assertTrue(section.activeImpulseFilters().isEmpty(), "Unsubscribe should deactivate filter");

        section.apply(new OutboxCreatedEvent(
                "IMPULSE",
                OutboxOperation.DELETE,
                filterId,
                userId,
                OffsetDateTime.now(),
                null
        ));

        assertTrue(section.activeImpulseFilters().isEmpty());
    }
}