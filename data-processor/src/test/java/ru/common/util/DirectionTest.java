package ru.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Тесты для enum {@link Direction}.
 */
class DirectionTest {

    @Test
    void allDirections_present() {
        Direction[] directions = Direction.values();
        assertEquals(3, directions.length);
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    void allDirections_notNull(Direction direction) {
        assertNotNull(direction);
    }

    @Test
    void up_direction() {
        assertEquals(Direction.UP, Direction.valueOf("UP"));
    }

    @Test
    void down_direction() {
        assertEquals(Direction.DOWN, Direction.valueOf("DOWN"));
    }

    @Test
    void both_direction() {
        assertEquals(Direction.BOTH, Direction.valueOf("BOTH"));
    }

    @Test
    void up_ordinal() {
        assertEquals(0, Direction.UP.ordinal());
    }

    @Test
    void down_ordinal() {
        assertEquals(1, Direction.DOWN.ordinal());
    }

    @Test
    void both_ordinal() {
        assertEquals(2, Direction.BOTH.ordinal());
    }

    @Test
    void valueOf_caseSensitive() {
        // Проверяем, что valueOf чувствителен к регистру
        Direction up = Direction.valueOf("UP");
        Direction down = Direction.valueOf("DOWN");
        Direction both = Direction.valueOf("BOTH");

        assertEquals(Direction.UP, up);
        assertEquals(Direction.DOWN, down);
        assertEquals(Direction.BOTH, both);
    }
}
