package ru.flink.models;

import org.junit.jupiter.api.Test;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImpulseRuntimeFilterTest {

    @Test
    void subscribe_addsSubscriberAndKeepsOriginalUnchanged() {
        ImpulseRuntimeFilter original = filter(Set.of(1L));

        ImpulseRuntimeFilter updated = original.subscribe(2L);

        assertEquals(Set.of(1L), original.subscribers());
        assertEquals(Set.of(1L, 2L), updated.subscribers());
        assertEquals(original.filterId(), updated.filterId());
        assertEquals(original.payload(), updated.payload());
    }

    @Test
    void unsubscribe_removesSubscriberAndKeepsOriginalUnchanged() {
        ImpulseRuntimeFilter original = filter(Set.of(1L, 2L));

        ImpulseRuntimeFilter updated = original.unsubscribe(2L);

        assertEquals(Set.of(1L, 2L), original.subscribers());
        assertEquals(Set.of(1L), updated.subscribers());
    }

    @Test
    void subscribe_existingSubscriber_isIdempotent() {
        ImpulseRuntimeFilter original = filter(Set.of(1L));

        ImpulseRuntimeFilter updated = original.subscribe(1L);

        assertEquals(Set.of(1L), updated.subscribers());
    }

    private static ImpulseRuntimeFilter filter(Set<Long> subscribers) {
        return new ImpulseRuntimeFilter(10L, payload(), subscribers);
    }

    private static OutboxPayload.ImpulseFilter payload() {
        return new OutboxPayload.ImpulseFilter(
                Set.of("binance"),
                Set.of("futures"),
                Set.of("ETH"),
                60,
                Direction.UP,
                10,
                0
        );
    }
}
