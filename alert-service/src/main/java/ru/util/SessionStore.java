package ru.util;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.kora.common.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public final class SessionStore {
    private static final Logger log = LoggerFactory.getLogger(SessionStore.class);

    private final ConcurrentMap<Integer, Set<Channel>> byUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<Channel, Integer> byChannel = new ConcurrentHashMap<>();

    public void register(int userId, Channel channel) {
        byUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(channel);
        byChannel.put(channel, userId);

        channel.closeFuture().addListener(future -> remove(channel));
        log.info("Зарегистрировали пользователя {}", userId);
    }

    public Integer userIdOf(Channel channel) {
        return byChannel.get(channel);
    }

    public List<Channel> activeChannelsOf(int userId) {

        Set<Channel> channels = byUser.get(userId);
        if (channels == null || channels.isEmpty()) return List.of();
        List<Channel> active = new ArrayList<>(channels.size());
        channels.stream().filter(Channel::isActive).forEach(active::add);

        return active;
    }

    public void remove(Channel channel) {
        Integer userId = byChannel.remove(channel);
        if (userId == null) return;

        byUser.computeIfPresent(userId, (id, channels) -> {
            channels.remove(channel);
            return channels.isEmpty() ? null : channels;
        });
        log.info("Отписали пользователя {}", userId);
    }

    public int onlineUsers() {
        return byUser.size();
    }

    public int totalChannels() {
        return byChannel.size();
    }
}
