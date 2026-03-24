package ru.core.trades.impulse;

import org.junit.jupiter.api.Test;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.common.util.OutboxOperation;
import ru.core.FilterKey;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpulseTradesSectionExtendedTest {

    private static final String ACTION = "IMPULSE";

    @Test
    void twoSubscribers_bothAppearInActiveSnapshot() {
        var section = new ImpulseTradesSection();
        long filterId = 7;
        var payload = impulsePayload(Direction.DOWN);

        section.apply(event(OutboxOperation.CREATE, filterId, 1, payload));
        section.apply(event(OutboxOperation.SUBSCRIBE, filterId, 10, null));
        section.apply(event(OutboxOperation.SUBSCRIBE, filterId, 20, null));

        var active = section.activeImpulseFilters();
        assertEquals(1, active.size());
        assertEquals(Set.of(10, 20), active.getFirst().subscribers());
        assertEquals(Direction.DOWN, active.getFirst().payload().direction());
    }

    @Test
    void subscribeSameUserTwice_singleSubscriberInSet() {
        var section = new ImpulseTradesSection();
        long filterId = 3;
        var payload = impulsePayload(Direction.UP);

        section.apply(event(OutboxOperation.CREATE, filterId, 1, payload));
        section.apply(event(OutboxOperation.SUBSCRIBE, filterId, 42, null));
        section.apply(event(OutboxOperation.SUBSCRIBE, filterId, 42, null));

        assertEquals(1, section.activeImpulseFilters().getFirst().subscribers().size());
        assertTrue(section.activeImpulseFilters().getFirst().subscribers().contains(42));
    }

    @Test
    void unsubscribeUserWhoWasNotSubscribed_doesNotBreakActiveSubscribers() {
        var section = new ImpulseTradesSection();
        long filterId = 5;
        section.apply(event(OutboxOperation.CREATE, filterId, 1, impulsePayload(Direction.UP)));
        section.apply(event(OutboxOperation.SUBSCRIBE, filterId, 1, null));

        section.apply(event(OutboxOperation.UNSUBSCRIBE, filterId, 999, null));

        assertEquals(1, section.activeImpulseFilters().size());
        assertEquals(Set.of(1), section.activeImpulseFilters().getFirst().subscribers());
    }

    @Test
    void delete_removesFilterFromInternalMap() {
        var section = new ImpulseTradesSection();
        long filterId = 11;
        section.apply(event(OutboxOperation.CREATE, filterId, 1, impulsePayload(Direction.UP)));
        section.apply(event(OutboxOperation.SUBSCRIBE, filterId, 1, null));

        section.apply(event(OutboxOperation.DELETE, filterId, 1, null));

        assertTrue(section.activeImpulseFilters().isEmpty());
        assertNull(section.get(new FilterKey(ACTION, filterId)));
    }

    @Test
    void secondCreateOnSameFilterId_replacesState_clearsSubscribers() {
        var section = new ImpulseTradesSection();
        long filterId = 13;
        section.apply(event(OutboxOperation.CREATE, filterId, 1,
                impulsePayloadWithPercent(Direction.UP, 5)));
        section.apply(event(OutboxOperation.SUBSCRIBE, filterId, 1, null));
        assertEquals(5, section.activeImpulseFilters().getFirst().payload().percent());

        section.apply(event(OutboxOperation.CREATE, filterId, 1,
                impulsePayloadWithPercent(Direction.UP, 9)));

        assertTrue(section.activeImpulseFilters().isEmpty());
        var state = section.get(new FilterKey(ACTION, filterId));
        assertNotNull(state);
        assertEquals(9, state.payload().percent());
        assertTrue(state.subscribers().isEmpty());
    }

    private static OutboxCreatedEvent event(
            OutboxOperation op, long filterId, int userId, OutboxPayload payload
    ) {
        return new OutboxCreatedEvent(
                ACTION,
                op,
                filterId,
                userId,
                OffsetDateTime.now(),
                payload
        );
    }

    private static OutboxPayload.ImpulseFilter impulsePayload(Direction d) {
        return new OutboxPayload.ImpulseFilter(
                List.of("binance"),
                List.of("futures"),
                List.of(),
                60,
                d,
                10,
                0
        );
    }

    private static OutboxPayload.ImpulseFilter impulsePayloadWithPercent(Direction d, int percent) {
        return new OutboxPayload.ImpulseFilter(
                List.of("binance"),
                List.of("futures"),
                List.of(),
                60,
                d,
                percent,
                0
        );
    }
}