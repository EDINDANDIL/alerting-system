package ru.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxTopicResolverTest {

    @Test
    void topicForAction_IMPULSE_returnsTrades() {
        assertEquals("Trades", OutboxTopicResolver.topicForAction("IMPULSE"));
    }

    @Test
    void topicForAction_nullAction_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> OutboxTopicResolver.topicForAction(null)
        );
        assertEquals("action must not be null or blank", ex.getMessage());
    }

    @Test
    void topicForAction_blankAction_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> OutboxTopicResolver.topicForAction("   ")
        );
        assertEquals("action must not be null or blank", ex.getMessage());
    }

    @Test
    void topicForAction_emptyAction_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> OutboxTopicResolver.topicForAction("")
        );
        assertEquals("action must not be null or blank", ex.getMessage());
    }

    @Test
    void topicForAction_unknownAction_throwsIllegalArgumentException() {
        String unknownAction = "VOLUME";
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> OutboxTopicResolver.topicForAction(unknownAction)
        );
        assertEquals("Unknown action for topic: " + unknownAction, ex.getMessage());
    }

    @Test
    void topicForAction_caseSensitive_differentCase_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OutboxTopicResolver.topicForAction("impulse")
        );
    }
}
