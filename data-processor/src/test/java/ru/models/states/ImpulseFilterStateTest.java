package ru.models.states;

import org.junit.jupiter.api.Test;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImpulseFilterStateTest {

    private static final OutboxPayload.ImpulseFilter DEFAULT_PAYLOAD =
            new OutboxPayload.ImpulseFilter(
                    List.of("binance"),
                    List.of("futures"),
                    List.of(),
                    60,
                    Direction.UP,
                    10,
                    0
            );

    @Test
    void initialState_emptySubscribers() {
        var state = new ImpulseFilterState(DEFAULT_PAYLOAD);
        assertEquals(DEFAULT_PAYLOAD, state.payload());
        assertTrue(state.subscribers().isEmpty());
    }

    @Test
    void addSubscriber_increasesSize() {
        var state = new ImpulseFilterState(DEFAULT_PAYLOAD);
        state.subscribers().add(100);
        assertEquals(1, state.subscribers().size());
        assertTrue(state.subscribers().contains(100));
    }

    @Test
    void addMultipleSubscribers_allPresent() {
        var state = new ImpulseFilterState(DEFAULT_PAYLOAD);
        state.subscribers().add(100);
        state.subscribers().add(200);
        state.subscribers().add(300);
        assertEquals(3, state.subscribers().size());
        assertEquals(Set.of(100, 200, 300), state.subscribers());
    }

    @Test
    void removeSubscriber_decreasesSize() {
        var state = new ImpulseFilterState(DEFAULT_PAYLOAD);
        state.subscribers().add(100);
        state.subscribers().add(200);
        state.subscribers().remove(100);
        assertEquals(1, state.subscribers().size());
        assertFalse(state.subscribers().contains(100));
        assertTrue(state.subscribers().contains(200));
    }

    @Test
    void removeNonExistentSubscriber_noChange() {
        var state = new ImpulseFilterState(DEFAULT_PAYLOAD);
        state.subscribers().add(100);
        state.subscribers().remove(999);
        assertEquals(1, state.subscribers().size());
        assertTrue(state.subscribers().contains(100));
    }

    @Test
    void duplicateSubscriber_noChange() {
        var state = new ImpulseFilterState(DEFAULT_PAYLOAD);
        state.subscribers().add(100);
        state.subscribers().add(100);
        assertEquals(1, state.subscribers().size());
    }

    @Test
    void clearSubscribers_removesAll() {
        var state = new ImpulseFilterState(DEFAULT_PAYLOAD);
        state.subscribers().add(100);
        state.subscribers().add(200);
        state.subscribers().clear();
        assertTrue(state.subscribers().isEmpty());
    }

    @Test
    void payload_immutable() {
        var state = new ImpulseFilterState(DEFAULT_PAYLOAD);
        OutboxPayload.ImpulseFilter payload = state.payload();
        assertEquals(List.of("binance"), payload.exchange());
        assertEquals(List.of("futures"), payload.market());
        assertEquals(60, payload.timeWindow());
        assertEquals(Direction.UP, payload.direction());
        assertEquals(10, payload.percent());
    }
}
