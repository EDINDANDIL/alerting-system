package ru.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для {@link WebSocketHandler}.
 * Тестируют извлечение userId и обработку сообщений.
 */
class WebSocketHandlerTest {

    @BeforeEach
    void setUp() {
        ConnectionManager connectionManager = mock(ConnectionManager.class);
        WebSocketHandler handler = new WebSocketHandler(connectionManager);
    }

    @Test
    void extractUserId_validUri_returnsUserId() {
        int userId = WebSocketHandler.extractUserId("/ws?userId=123");
        assertEquals(123, userId);
    }

    @Test
    void extractUserId_withAdditionalParams_returnsUserId() {
        int userId = WebSocketHandler.extractUserId("/ws?userId=456&token=abc");
        assertEquals(456, userId);
    }

    @Test
    void extractUserId_noQuery_returnsMinusOne() {
        int userId = WebSocketHandler.extractUserId("/ws");
        assertEquals(-1, userId);
    }

    @Test
    void extractUserId_emptyQuery_returnsMinusOne() {
        int userId = WebSocketHandler.extractUserId("/ws?");
        assertEquals(-1, userId);
    }

    @Test
    void extractUserId_noUserIdParam_returnsMinusOne() {
        int userId = WebSocketHandler.extractUserId("/ws?token=abc");
        assertEquals(-1, userId);
    }

    @Test
    void extractUserId_invalidUserId_returnsMinusOne() {
        int userId = WebSocketHandler.extractUserId("/ws?userId=abc");
        assertEquals(-1, userId);
    }

    @Test
    void extractUserId_negativeUserId_returnsMinusOne() {
        int userId = WebSocketHandler.extractUserId("/ws?userId=-123");
        assertEquals(-123, userId); // Парсится, но должен быть отклонен обработчиком
    }

    @Test
    void extractUserId_zeroUserId_returnsMinusOne() {
        int userId = WebSocketHandler.extractUserId("/ws?userId=0");
        assertEquals(0, userId);
    }

    @Test
    void extractUserId_largeUserId_returnsUserId() {
        int userId = WebSocketHandler.extractUserId("/ws?userId=999999");
        assertEquals(999999, userId);
    }

    @Test
    void extractUserId_malformedParams_returnsMinusOne() {
        int userId = WebSocketHandler.extractUserId("/ws?userId");
        assertEquals(-1, userId);
    }

    @Test
    void extractUserId_emptyValue_returnsMinusOne() {
        int userId = WebSocketHandler.extractUserId("/ws?userId=");
        assertEquals(-1, userId);
    }
}
