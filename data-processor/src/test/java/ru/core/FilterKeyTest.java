package ru.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FilterKeyTest {

    @Test
    void recordCreation_allFieldsAccessible() {
        var key = new FilterKey("IMPULSE", 42L);

        assertEquals("IMPULSE", key.action());
        assertEquals(42L, key.filterId());
    }

    @Test
    void record_equalsAndHashCode_sameValues() {
        var key1 = new FilterKey("IMPULSE", 100L);
        var key2 = new FilterKey("IMPULSE", 100L);

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void record_notEquals_differentAction() {
        var key1 = new FilterKey("IMPULSE", 100L);
        var key2 = new FilterKey("OTHER", 100L);

        assertNotEquals(key1, key2);
    }

    @Test
    void record_notEquals_differentFilterId() {
        var key1 = new FilterKey("IMPULSE", 100L);
        var key2 = new FilterKey("IMPULSE", 200L);

        assertNotEquals(key1, key2);
    }

    @Test
    void record_toString_containsFields() {
        var key = new FilterKey("IMPULSE", 999L);

        String str = key.toString();

        assertEquals("FilterKey[action=IMPULSE, filterId=999]", str);
    }
}
