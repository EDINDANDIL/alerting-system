package ru.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Тесты для enum {@link OutboxOperation}.
 */
class OutboxOperationTest {

    @Test
    void allOperations_present() {
        OutboxOperation[] operations = OutboxOperation.values();
        assertEquals(4, operations.length);
    }

    @ParameterizedTest
    @EnumSource(OutboxOperation.class)
    void allOperations_notNull(OutboxOperation operation) {
        assertNotNull(operation);
    }

    @Test
    void create_operation() {
        assertEquals(OutboxOperation.CREATE, OutboxOperation.valueOf("CREATE"));
    }

    @Test
    void subscribe_operation() {
        assertEquals(OutboxOperation.SUBSCRIBE, OutboxOperation.valueOf("SUBSCRIBE"));
    }

    @Test
    void unsubscribe_operation() {
        assertEquals(OutboxOperation.UNSUBSCRIBE, OutboxOperation.valueOf("UNSUBSCRIBE"));
    }

    @Test
    void delete_operation() {
        assertEquals(OutboxOperation.DELETE, OutboxOperation.valueOf("DELETE"));
    }

    @Test
    void create_ordinal() {
        assertEquals(0, OutboxOperation.CREATE.ordinal());
    }

    @Test
    void delete_ordinal() {
        assertEquals(1, OutboxOperation.DELETE.ordinal());
    }

    @Test
    void subscribe_ordinal() {
        assertEquals(2, OutboxOperation.SUBSCRIBE.ordinal());
    }

    @Test
    void unsubscribe_ordinal() {
        assertEquals(3, OutboxOperation.UNSUBSCRIBE.ordinal());
    }

    @Test
    void valueOf_caseSensitive() {
        OutboxOperation create = OutboxOperation.valueOf("CREATE");
        OutboxOperation subscribe = OutboxOperation.valueOf("SUBSCRIBE");
        OutboxOperation unsubscribe = OutboxOperation.valueOf("UNSUBSCRIBE");
        OutboxOperation delete = OutboxOperation.valueOf("DELETE");

        assertEquals(OutboxOperation.CREATE, create);
        assertEquals(OutboxOperation.SUBSCRIBE, subscribe);
        assertEquals(OutboxOperation.UNSUBSCRIBE, unsubscribe);
        assertEquals(OutboxOperation.DELETE, delete);
    }
}
