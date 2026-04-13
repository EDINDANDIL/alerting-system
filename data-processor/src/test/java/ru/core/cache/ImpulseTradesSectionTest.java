package ru.core.cache;

import org.junit.jupiter.api.Test;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;
import ru.common.util.OutboxOperation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImpulseTradesSectionTest {

    @Test
    void fullLifecycle_createSubscribeUnsubscribeDelete() {
        var section = new ImpulseTradesSection();
        long filterId = 1L;
        int userId = 1;

        var payload = impulsePayload(List.of(), 60, Direction.UP, 5);

        section.apply(event(OutboxOperation.CREATE, filterId, userId, payload));
        var afterCreate = section.activeImpulseFilters();
        assertEquals(1, afterCreate.size());
        assertTrue(afterCreate.getFirst().subscribers().isEmpty());

        section.apply(event(OutboxOperation.SUBSCRIBE, filterId, userId, null));
        var active = section.activeImpulseFilters();
        assertEquals(1, active.size());
        assertEquals(filterId, active.getFirst().filterId());
        assertTrue(active.getFirst().subscribers().contains(userId));

        section.apply(event(OutboxOperation.UNSUBSCRIBE, filterId, userId, null));
        var afterUnsub = section.activeImpulseFilters();
        assertEquals(1, afterUnsub.size());
        assertTrue(afterUnsub.getFirst().subscribers().isEmpty());

        section.apply(event(OutboxOperation.DELETE, filterId, userId, null));
        assertTrue(section.activeImpulseFilters().isEmpty());
    }

    @Test
    void filtersByWindow_populatedOnCreate() {
        var section = new ImpulseTradesSection();
        long filterId = 1L;

        var payload = impulsePayload(List.of(), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, filterId, 1, payload));

        long twNs = 60L * 1_000_000_000L;
        var byWindow = section.filtersByWindow();
        assertTrue(byWindow.containsKey(twNs));
        var filters = byWindow.get(twNs);
        assertEquals(1, filters.size());
        assertEquals(filterId, filters.iterator().next().filterId());
    }

    @Test
    void filtersByWindow_sameWindowSharesSet() {
        var section = new ImpulseTradesSection();

        var p1 = impulsePayload(List.of(), 60, Direction.UP, 5);
        var p2 = impulsePayload(List.of(), 60, Direction.DOWN, 10);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, p1));
        section.apply(event(OutboxOperation.CREATE, 2L, 1, p2));

        long twNs = 60L * 1_000_000_000L;
        var filters = section.filtersByWindow().get(twNs);
        assertNotNull(filters);
        assertEquals(2, filters.size());
    }

    @Test
    void filtersByWindow_differentWindowsSeparate() {
        var section = new ImpulseTradesSection();

        var p1 = impulsePayload(List.of(), 5, Direction.UP, 5);
        var p2 = impulsePayload(List.of(), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, p1));
        section.apply(event(OutboxOperation.CREATE, 2L, 1, p2));

        assertEquals(2, section.filtersByWindow().size());
    }

    @Test
    void filtersByWindow_removedOnDelete() {
        var section = new ImpulseTradesSection();

        var payload = impulsePayload(List.of(), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, payload));
        section.apply(event(OutboxOperation.DELETE, 1L, 1, null));

        long twNs = 60L * 1_000_000_000L;
        var filters = section.filtersByWindow().get(twNs);
        assertNull(filters);
    }

    @Test
    void filtersByWindow_subscribersUpdatedAfterSubscribe() {
        var section = new ImpulseTradesSection();

        var payload = impulsePayload(List.of(), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, payload));
        section.apply(event(OutboxOperation.SUBSCRIBE, 1L, 1, null));

        long twNs = 60L * 1_000_000_000L;
        var filters = section.filtersByWindow().get(twNs);
        var view = filters.iterator().next();
        assertTrue(view.subscribers().contains(1));
    }

    @Test
    void blacklist_populatedWhenFilterHasBlackList() {
        var section = new ImpulseTradesSection();

        var payload = impulsePayload(List.of("btcusdt"), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, payload));

        var bl = section.blacklistFor("btcusdt");
        assertNotNull(bl);
        assertTrue(bl.contains(1L));
    }

    @Test
    void blacklist_emptyWhenSymbolNotBlackListed() {
        var section = new ImpulseTradesSection();

        var payload = impulsePayload(List.of("btcusdt"), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, payload));

        assertNull(section.blacklistFor("ethusdt"));
    }

    @Test
    void blacklist_removedWhenFilterDeleted() {
        var section = new ImpulseTradesSection();

        var payload = impulsePayload(List.of("btcusdt"), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, payload));
        section.apply(event(OutboxOperation.DELETE, 1L, 1, null));

        assertNull(section.blacklistFor("btcusdt"));
    }

    @Test
    void blacklist_multipleFiltersSameSymbol() {
        var section = new ImpulseTradesSection();

        var p1 = impulsePayload(List.of("btcusdt"), 60, Direction.UP, 5);
        var p2 = impulsePayload(List.of("btcusdt"), 60, Direction.DOWN, 10);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, p1));
        section.apply(event(OutboxOperation.CREATE, 2L, 1, p2));

        var bl = section.blacklistFor("btcusdt");
        assertNotNull(bl);
        assertEquals(Set.of(1L, 2L), bl);
    }

    @Test
    void hasAnySubscriber_returnsFalseWhenNoSubscribers() {
        var section = new ImpulseTradesSection();
        var payload = impulsePayload(List.of(), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, payload));
        assertFalse(section.hasAnySubscriber());
    }

    @Test
    void hasAnySubscriber_returnsTrueAfterSubscribe() {
        var section = new ImpulseTradesSection();
        var payload = impulsePayload(List.of(), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, payload));
        section.apply(event(OutboxOperation.SUBSCRIBE, 1L, 100, null));
        assertTrue(section.hasAnySubscriber());
    }

    @Test
    void hasAnySubscriber_returnsFalseAfterAllUnsubscribe() {
        var section = new ImpulseTradesSection();
        var payload = impulsePayload(List.of(), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, payload));
        section.apply(event(OutboxOperation.SUBSCRIBE, 1L, 100, null));
        section.apply(event(OutboxOperation.UNSUBSCRIBE, 1L, 100, null));
        assertFalse(section.hasAnySubscriber());
    }

    @Test
    void isBlacklisted_returnsTrueForBlacklistedSymbol() {
        var section = new ImpulseTradesSection();
        var payload = impulsePayload(List.of("btcusdt"), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, payload));
        assertTrue(section.isBlacklisted("btcusdt", 1L));
    }

    @Test
    void isBlacklisted_returnsFalseForNonBlacklistedSymbol() {
        var section = new ImpulseTradesSection();
        var payload = impulsePayload(List.of("btcusdt"), 60, Direction.UP, 5);
        section.apply(event(OutboxOperation.CREATE, 1L, 1, payload));
        assertFalse(section.isBlacklisted("ethusdt", 1L));
    }

    @Test
    void deleteNonExistentFilter_noError() {
        var section = new ImpulseTradesSection();
        section.apply(event(OutboxOperation.DELETE, 999L, 1, null));
        assertTrue(section.activeImpulseFilters().isEmpty());
    }

    @Test
    void subscribeNonExistentFilter_noError() {
        var section = new ImpulseTradesSection();
        section.apply(event(OutboxOperation.SUBSCRIBE, 999L, 1, null));
        assertTrue(section.activeImpulseFilters().isEmpty());
    }

    // ==================== helpers ====================

    private static OutboxPayload.ImpulseFilter impulsePayload(
            List<String> blackList, int timeWindowSec, Direction direction, int percent) {
        return new OutboxPayload.ImpulseFilter(
                List.of("binance"), List.of("futures"), blackList,
                timeWindowSec, direction, percent, 0);
    }

    private static OutboxCreatedEvent event(
            OutboxOperation op, long filterId, int userId, OutboxPayload payload) {
        return new OutboxCreatedEvent(
                "IMPULSE", op, filterId, userId, OffsetDateTime.now(), payload);
    }
}
