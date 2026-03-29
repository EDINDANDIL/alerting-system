package ru.services;

import io.netty.channel.Channel;
import ru.models.dto.AlertEvent;
import ru.models.dto.AlertMessage;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ConnectionManager {
    CompletableFuture<Void> sendToUser(int userId, AlertMessage alert);

    CompletableFuture<Void> sendToAll(Set<Integer> userIds, AlertMessage alert);

    void register(int userId, Channel channel);

    void unregister(Channel channel);
}
