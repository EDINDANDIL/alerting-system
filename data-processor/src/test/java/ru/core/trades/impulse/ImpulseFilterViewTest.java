package ru.core.trades.impulse;

import org.junit.jupiter.api.Test;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpulseFilterViewTest {

    private static final OutboxPayload.ImpulseFilter PAYLOAD =
            new OutboxPayload.ImpulseFilter(
                    List.of("binance"),
                    List.of("futures"),
                    List.of("ethusdt"),
                    60,
                    Direction.BOTH,
                    15,
                    0
            );

    @Test
    void recordCreation_allFieldsAccessible() {
        var view = new ImpulseFilterView(42L, PAYLOAD, Set.of(1, 2, 3));

        assertEquals(42L, view.filterId());
        assertEquals(PAYLOAD, view.payload());
        assertEquals(Set.of(1, 2, 3), view.subscribers());
    }

    @Test
    void record_equalsAndHashCode() {
        var view1 = new ImpulseFilterView(1L, PAYLOAD, Set.of(100));
        var view2 = new ImpulseFilterView(1L, PAYLOAD, Set.of(100));
        var view3 = new ImpulseFilterView(2L, PAYLOAD, Set.of(100));

        assertEquals(view1, view2);
        assertEquals(view1.hashCode(), view2.hashCode());
        // Разные filterId -> не равны
        assertEquals(false, view1.equals(view3));
    }

    @Test
    void record_withEmptySubscribers() {
        var view = new ImpulseFilterView(1L, PAYLOAD, Set.of());

        assertEquals(1L, view.filterId());
        assertTrue(view.subscribers().isEmpty());
    }

    @Test
    void record_withMultipleSubscribers() {
        var view = new ImpulseFilterView(999L, PAYLOAD, Set.of(10, 20, 30, 40));

        assertEquals(999L, view.filterId());
        assertEquals(4, view.subscribers().size());
    }

    @Test
    void payload_fieldsAccessible() {
        var view = new ImpulseFilterView(1L, PAYLOAD, Set.of(1));

        OutboxPayload.ImpulseFilter payload = view.payload();
        assertEquals(List.of("binance"), payload.exchange());
        assertEquals(List.of("futures"), payload.market());
        assertEquals(List.of("ethusdt"), payload.blackList());
        assertEquals(60, payload.timeWindow());
        assertEquals(Direction.BOTH, payload.direction());
        assertEquals(15, payload.percent());
    }
}
