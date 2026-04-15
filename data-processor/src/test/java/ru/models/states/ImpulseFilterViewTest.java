package ru.models.states;

import org.junit.jupiter.api.Test;
import ru.common.dto.OutboxPayload;
import ru.common.util.Direction;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImpulseFilterViewTest {

    private static final OutboxPayload.ImpulseFilter PAYLOAD =
            new OutboxPayload.ImpulseFilter(
                    Set.of("binance"),
                    Set.of("futures"),
                    Set.of("ethusdt"),
                    60,
                    Direction.BOTH,
                    15,
                    0
            );

    @Test
    void recordCreation_allFieldsAccessible() {
        var view = new ImpulseFilterView(42L, PAYLOAD, Set.of(1L, 2L, 3L));
        assertEquals(42L, view.filterId());
        assertEquals(PAYLOAD, view.payload());
        assertEquals(Set.of(1L, 2L, 3L), view.subscribers());
        assertEquals(3, view.subscribers().size());
    }

    @Test
    void record_equalsAndHashCode() {
        var view1 = new ImpulseFilterView(1L, PAYLOAD, Set.of(100L));
        var view2 = new ImpulseFilterView(1L, PAYLOAD, Set.of(100L));
        var view3 = new ImpulseFilterView(2L, PAYLOAD, Set.of(100L));
        assertEquals(1L, view1.filterId());
        assertEquals(view1, view2);
        assertEquals(view1.hashCode(), view2.hashCode());
        assertNotEquals(view1, view3);
    }

    @Test
    void payload_fieldsAccessible() {
        var view = new ImpulseFilterView(1L, PAYLOAD, Set.of(1L));
        OutboxPayload.ImpulseFilter payload = view.payload();
        assertEquals(Set.of("binance"), payload.exchange());
        assertEquals(Set.of("futures"), payload.market());
        assertEquals(Set.of("ethusdt"), payload.blackList());
        assertEquals(60, payload.timeWindow());
        assertEquals(Direction.BOTH, payload.direction());
        assertEquals(15, payload.percent());
    }
}
