package ru.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.common.dto.AlertCreatedEvent;
import ru.tinkoff.kora.common.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

@Component
public final class AlertStreamService {

    private final Map<Long, List<SubmissionPublisher<ByteBuffer>>> connections = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public Flow.Publisher<ByteBuffer> connect(long userId) {
        SubmissionPublisher<ByteBuffer> publisher = new SubmissionPublisher<>();

        connections.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>())
                .add(publisher);

        publisher.submit(event(":connected"));

        return publisher;
    }

    public void send(AlertCreatedEvent alert) {
        byte[] bytes;
        try {
            bytes = event(mapper.writeValueAsString(alert)).array();
        } catch (Exception e) {
            return;
        }

        for (Long userId : alert.subscribers()) {
            List<SubmissionPublisher<ByteBuffer>> userConnections = connections.get(userId);
            if (userConnections == null) continue;

            for (SubmissionPublisher<ByteBuffer> publisher : userConnections) {
                publisher.submit(ByteBuffer.wrap(bytes));
            }
        }
    }

    private static ByteBuffer event(String data) {
        return ByteBuffer.wrap(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
    }
}