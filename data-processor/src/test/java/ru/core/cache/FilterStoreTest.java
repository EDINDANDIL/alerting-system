package ru.core.cache;

import org.junit.jupiter.api.Test;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.models.states.ImpulseFilterView;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FilterStoreTest {

    @Test
    void getAll_returnsExistingFilters() {
        var store = new FilterStore();
        var view = new ImpulseFilterView(
            1L, impulsePayload(Direction.UP, 10), Set.of(100L));
        store.put(1L, view);

        var result = store.getAll(Set.of(1L));
        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().filterId());
    }

    @Test
    void getAll_skipsMissingFilters() {
        var store = new FilterStore();
        var view = new ImpulseFilterView(
            1L, impulsePayload(Direction.UP, 10), Set.of(100L));
        store.put(1L, view);

        var result = store.getAll(Set.of(1L, 999L));
        assertEquals(1, result.size());
    }

    @Test
    void getAll_emptySet() {
        var store = new FilterStore();
        assertTrue(store.getAll(Set.of()).isEmpty());
    }

    @Test
    void getAll_unknownIds() {
        var store = new FilterStore();
        assertTrue(store.getAll(Set.of(1L, 2L, 3L)).isEmpty());
    }

    @Test
    void hasAnySubscriber_falseWhenEmpty() {
        var store = new FilterStore();
        assertFalse(store.hasAnySubscriber());
    }

    @Test
    void hasAnySubscriber_trueAfterPutWithSubscribers() {
        var store = new FilterStore();
        store.put(1L, new ImpulseFilterView(
            1L, impulsePayload(Direction.UP, 10), Set.of(100L)));
        assertTrue(store.hasAnySubscriber());
    }

    @Test
    void hasAnySubscriber_falseWhenSubscribersEmpty() {
        var store = new FilterStore();
        store.put(1L, new ImpulseFilterView(
            1L, impulsePayload(Direction.UP, 10), Set.of()));
        assertFalse(store.hasAnySubscriber());
    }

    @Test
    void putAndGet_roundTrip() {
        var store = new FilterStore();
        var view = new ImpulseFilterView(
            42L, impulsePayload(Direction.DOWN, 5), Set.of(1L, 2L));
        store.put(42L, view);

        var got = store.get(42L);
        assertNotNull(got);
        assertEquals(42L, got.filterId());
        assertEquals(2, got.subscribers().size());
    }

    @Test
    void get_unknownId_returnsNull() {
        var store = new FilterStore();
        assertNull(store.get(999L));
    }

    @Test
    void remove_removesFilter() {
        var store = new FilterStore();
        store.put(1L, new ImpulseFilterView(
            1L, impulsePayload(Direction.UP, 10), Set.of(100L)));
        store.remove(1L);
        assertNull(store.get(1L));
    }

    @Test
    void remove_nonExistent_noError() {
        var store = new FilterStore();
        store.remove(999L);
    }

    @Test
    void updateSubscribers_replacesView() {
        var store = new FilterStore();
        var old = new ImpulseFilterView(
            1L, impulsePayload(Direction.UP, 10), Set.of(100L));
        store.put(1L, old);

        var updated = new ImpulseFilterView(
            1L, impulsePayload(Direction.UP, 10), Set.of(100L, 200L));
        store.put(1L, updated);

        var got = store.get(1L);
        assertEquals(2, got.subscribers().size());
    }

    // ==================== helpers ====================

    private static OutboxPayload.ImpulseFilter impulsePayload(
            Direction direction, int percent) {
        return new OutboxPayload.ImpulseFilter(
                Set.of("binance"), Set.of("futures"), Set.of(),
                60, direction, percent, 0);
    }
}
