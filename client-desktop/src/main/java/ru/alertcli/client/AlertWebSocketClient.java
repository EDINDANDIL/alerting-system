package ru.alertcli.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.tyrus.client.ClientManager;
import ru.alertcli.model.AlertMessage;

import jakarta.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket клиент для подключения к alert-service.
 * ws://localhost:{port}/ws?userId={userId}
 */
public class AlertWebSocketClient {
    private final ObjectMapper objectMapper;
    private Session session;
    private final BlockingQueue<AlertMessage> alertQueue;
    private final AtomicBoolean connected;

    public AlertWebSocketClient() {
        this.objectMapper = new ObjectMapper();
        this.alertQueue = new LinkedBlockingQueue<>();
        this.connected = new AtomicBoolean(false);
    }

    public boolean connect(String host, int port, int userId) {
        try {
            ClientManager client = ClientManager.createClient();
            String uri = "ws://" + host + ":" + port + "/ws?userId=" + userId;

            client.connectToServer(new AlertEndpoint(), new URI(uri));

            // Ждём подтверждения подключения
            for (int i = 0; i < 30 && !connected.get(); i++) {
                Thread.sleep(100);
            }

            return connected.get();
        } catch (Exception e) {
            System.err.println("WebSocket connection failed: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        connected.set(false);
        if (session != null && session.isOpen()) {
            try {
                session.close();
                System.out.println("Disconnected from WebSocket");
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return connected.get() && session != null && session.isOpen();
    }

    public AlertMessage pollAlert() {
        try {
            return alertQueue.poll(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @ClientEndpoint
    private class AlertEndpoint extends Endpoint {

        @Override
        public void onOpen(Session session, EndpointConfig endpointConfig) {
            AlertWebSocketClient.this.session = session;
            connected.set(true);
            System.out.println("Connected to alert-service (userId=" + extractUserId(session) + ")");

            // Регистрируем обработчик сообщений
            // Игнорируем pong
            MessageHandler.Whole<String> messageHandler = message -> {
                if ("pong".equalsIgnoreCase(message)) {
                    return; // Игнорируем pong
                }
                try {
                    AlertMessage alert = objectMapper.readValue(message, AlertMessage.class);
                    alertQueue.offer(alert);
                } catch (Exception e) {
                    System.err.println("Failed to parse alert: " + e.getMessage());
                    System.err.println("   Raw: " + message);
                }
            };
            session.addMessageHandler(messageHandler);
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            connected.set(false);
            System.out.println("Disconnected: " + closeReason.getReasonPhrase());
        }

        @Override
        public void onError(Session session, Throwable throwable) {
            System.err.println("WebSocket error: " + throwable.getMessage());
        }

        private int extractUserId(Session session) {
            try {
                String uri = session.getRequestURI().toString();
                int queryIndex = uri.indexOf('?');
                if (queryIndex >= 0) {
                    String query = uri.substring(queryIndex + 1);
                    String[] params = query.split("&");
                    for (String param : params) {
                        String[] pair = param.split("=");
                        if (pair.length == 2 && "userId".equals(pair[0])) {
                            return Integer.parseInt(pair[1]);
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
            return -1;
        }
    }
}
