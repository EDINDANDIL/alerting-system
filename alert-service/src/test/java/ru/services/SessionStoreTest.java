package ru.services;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.util.SessionStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link SessionStore}.
 * Тестируют хранение и управление сессиями пользователей.
 */
class SessionStoreTest {

    private SessionStore sessionStore;

    @BeforeEach
    void setUp() {
        sessionStore = new SessionStore();
    }

    @Test
    void register_singleChannel_storedCorrectly() {
        Channel channel = new EmbeddedChannel();
        sessionStore.register(123, channel);

        assertEquals(123, sessionStore.userIdOf(channel));
        assertEquals(1, sessionStore.onlineUsers());
        assertEquals(1, sessionStore.totalChannels());
    }

    @Test
    void register_multipleChannelsForSameUser_allStored() {
        Channel channel1 = new EmbeddedChannel();
        Channel channel2 = new EmbeddedChannel();

        sessionStore.register(123, channel1);
        sessionStore.register(123, channel2);

        assertEquals(123, sessionStore.userIdOf(channel1));
        assertEquals(123, sessionStore.userIdOf(channel2));
        assertEquals(1, sessionStore.onlineUsers());
        assertEquals(2, sessionStore.totalChannels());
    }

    @Test
    void register_multipleUsers_allStored() {
        Channel channel1 = new EmbeddedChannel();
        Channel channel2 = new EmbeddedChannel();

        sessionStore.register(123, channel1);
        sessionStore.register(456, channel2);

        assertEquals(123, sessionStore.userIdOf(channel1));
        assertEquals(456, sessionStore.userIdOf(channel2));
        assertEquals(2, sessionStore.onlineUsers());
        assertEquals(2, sessionStore.totalChannels());
    }

    @Test
    void activeChannelsOf_singleActiveChannel_returnsChannel() {
        Channel channel = new EmbeddedChannel();
        sessionStore.register(123, channel);

        List<Channel> channels = sessionStore.activeChannelsOf(123);

        assertEquals(1, channels.size());
        assertTrue(channels.contains(channel));
    }

    @Test
    void activeChannelsOf_inactiveChannel_notReturned() {
        Channel channel = new EmbeddedChannel();
        sessionStore.register(123, channel);
        channel.close();

        List<Channel> channels = sessionStore.activeChannelsOf(123);

        assertTrue(channels.isEmpty());
    }

    @Test
    void activeChannelsOf_nonExistentUser_returnsEmpty() {
        List<Channel> channels = sessionStore.activeChannelsOf(999);
        assertTrue(channels.isEmpty());
    }

    @Test
    void remove_channelRemoved_fromBothMaps() {
        Channel channel = new EmbeddedChannel();
        sessionStore.register(123, channel);

        sessionStore.remove(channel);

        assertNull(sessionStore.userIdOf(channel));
        assertEquals(0, sessionStore.onlineUsers());
        assertEquals(0, sessionStore.totalChannels());
    }

    @Test
    void remove_oneOfMultipleChannels_onlyOneRemoved() {
        Channel channel1 = new EmbeddedChannel();
        Channel channel2 = new EmbeddedChannel();

        sessionStore.register(123, channel1);
        sessionStore.register(123, channel2);

        sessionStore.remove(channel1);

        assertNull(sessionStore.userIdOf(channel1));
        assertEquals(123, sessionStore.userIdOf(channel2));
        assertEquals(1, sessionStore.onlineUsers());
        assertEquals(1, sessionStore.totalChannels());
    }

    @Test
    void remove_nonExistentChannel_noError() {
        Channel channel = new EmbeddedChannel();
        sessionStore.remove(channel); // Не должно вызывать ошибок

        assertEquals(0, sessionStore.onlineUsers());
        assertEquals(0, sessionStore.totalChannels());
    }

    @Test
    void userIdOf_nonExistentChannel_returnsNull() {
        Channel channel = new EmbeddedChannel();
        Integer userId = sessionStore.userIdOf(channel);
        assertNull(userId);
    }

    @Test
    void onlineUsers_correctCount() {
        Channel channel1 = new EmbeddedChannel();
        Channel channel2 = new EmbeddedChannel();
        Channel channel3 = new EmbeddedChannel();

        sessionStore.register(123, channel1);
        sessionStore.register(123, channel2); // Тот же пользователь
        sessionStore.register(456, channel3); // Новый пользователь

        assertEquals(2, sessionStore.onlineUsers());
    }

    @Test
    void totalChannels_correctCount() {
        Channel channel1 = new EmbeddedChannel();
        Channel channel2 = new EmbeddedChannel();
        Channel channel3 = new EmbeddedChannel();

        sessionStore.register(123, channel1);
        sessionStore.register(123, channel2);
        sessionStore.register(456, channel3);

        assertEquals(3, sessionStore.totalChannels());
    }

    @Test
    void remove_lastChannelForUser_userRemoved() {
        Channel channel1 = new EmbeddedChannel();
        Channel channel2 = new EmbeddedChannel();

        sessionStore.register(123, channel1);
        sessionStore.register(456, channel2);

        sessionStore.remove(channel1);

        assertEquals(1, sessionStore.onlineUsers());
        assertEquals(1, sessionStore.totalChannels());
    }
}
