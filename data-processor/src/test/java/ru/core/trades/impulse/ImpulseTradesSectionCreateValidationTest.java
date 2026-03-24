package ru.core.trades.impulse;

import org.junit.jupiter.api.Test;
import ru.common.dto.OutboxCreatedEvent;
import ru.common.util.OutboxOperation;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

// тест на исключения, краевые случаи
class ImpulseTradesSectionCreateValidationTest {

    @Test
    void CRUD_Throws_1() {
        var section = new ImpulseTradesSection();

        var e = new OutboxCreatedEvent(
                "IMPULSE",
                OutboxOperation.CREATE,
                1L,
                10,
                OffsetDateTime.now(),
                null
        );

        var ex = assertThrows(IllegalArgumentException.class, () -> section.apply(e));
        assertTrue(ex.getMessage().contains("CREATE IMPULSE"), ex.getMessage());
    }

    @Test
    void CRUD_Throws_2() {
        var section = new ImpulseTradesSection();

        assertDoesNotThrow(() -> section.apply(new OutboxCreatedEvent(
                "IMPULSE",
                OutboxOperation.SUBSCRIBE,
                999L,
                42,
                OffsetDateTime.now(),
                null
        )));

        assertTrue(section.activeImpulseFilters().isEmpty());
    }
}