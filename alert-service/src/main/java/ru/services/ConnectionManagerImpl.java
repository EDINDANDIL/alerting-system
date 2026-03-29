package ru.services;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.models.dto.AlertMessage;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.json.common.JsonWriter;
import ru.util.SessionStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
public final class ConnectionManagerImpl implements ConnectionManager {
    private static final Logger log = LoggerFactory.getLogger(ConnectionManagerImpl.class);

    private final SessionStore sessionStore;
    private final JsonWriter<AlertMessage> alertWriter;
    private final WebSocketServer webSocketServer;
    private final JsonFactory jsonFactory = new JsonFactory();

    public ConnectionManagerImpl(SessionStore sessionStore, JsonWriter<AlertMessage> alertWriter, WebSocketServer webSocketServer) {
        this.sessionStore = sessionStore;
        this.alertWriter = alertWriter;
        this.webSocketServer = webSocketServer;
    }

    @Override
    public CompletableFuture<Void> sendToUser(int userId, AlertMessage alert) {
        List<Channel> channels = sessionStore.activeChannelsOf(userId);

        if (channels.isEmpty()) return CompletableFuture.completedFuture(null);

        String message;
        try {
            message = toJson(alert);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        List<CompletableFuture<Void>> writes = new ArrayList<>(channels.size());

        for (Channel channel : channels) {
            CompletableFuture<Void> writeFuture = new CompletableFuture<>();

            channel.writeAndFlush(new TextWebSocketFrame(message))
                    .addListener((ChannelFutureListener) f -> {
                        if (f.isSuccess()) {
                            writeFuture.complete(null);
                        } else {
                            writeFuture.completeExceptionally(f.cause());
                        }
                    });

            writes.add(writeFuture);
        }

        return CompletableFuture.allOf(writes.toArray(new CompletableFuture[0]))
                .whenComplete((v, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to send alert to user {} for symbol {}", userId, alert.symbol(), ex);
                    } else {
                        log.debug("Delivered alert to user {} via {} channel(s)", userId, channels.size());
                    }
                });
    }

    private String toJson(AlertMessage alert) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonGenerator gen = jsonFactory.createGenerator(baos)) {
            alertWriter.write(gen, alert);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    @Override
    public CompletableFuture<Void> sendToAll(Set<Integer> userIds, AlertMessage alert) {
        List<CompletableFuture<Void>> futures = new ArrayList<>(userIds.size());

        for (Integer userId : userIds) {
            futures.add(sendToUser(userId, alert));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, ex) -> {
                    if (ex != null) {
                        log.error("Some deliveries failed for alert symbol={}", alert.symbol(), ex);
                    } else {
                        log.info("Delivered alert symbol={} to {} users", alert.symbol(), userIds.size());
                    }
                });
    }

    @Override
    public void register(int userId, Channel channel) {
        sessionStore.register(userId, channel);
        log.info("User {} connected. onlineUsers={}, channels={}",
                userId, sessionStore.onlineUsers(), sessionStore.totalChannels());
    }

    @Override
    public void unregister(Channel channel) {
        Integer userId = sessionStore.userIdOf(channel);
        sessionStore.remove(channel);
        if (userId != null) {
            log.info("User {} disconnected. onlineUsers={}, channels={}",
                    userId, sessionStore.onlineUsers(), sessionStore.totalChannels());
        }
    }
}
