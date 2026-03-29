package ru.alertcli.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.tyrus.client.ClientManager;

import jakarta.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket клиент для Tiger API.
 * Отправляет команду setLinkSymbol для открытия инструмента в группе линковки.
 * ws://localhost:7819
 */
public class TigerWebSocketClient {
    private final ObjectMapper objectMapper;
    private Session session;
    private final AtomicBoolean connected;
    private final String linkGroup;

    public TigerWebSocketClient(String linkGroup) {
        this.objectMapper = new ObjectMapper();
        this.connected = new AtomicBoolean(false);
        this.linkGroup = linkGroup != null ? linkGroup : "A";
    }

    /**
     * Подключиться к Tiger API
     */
    public boolean connect(String host, int port) {
        try {
            ClientManager client = ClientManager.createClient();
            String uri = "ws://" + host + ":" + port;

            client.connectToServer(new TigerEndpoint(), new URI(uri));

            // Ждём подтверждения подключения
            for (int i = 0; i < 30 && !connected.get(); i++) {
                Thread.sleep(100);
            }

            return connected.get();
        } catch (Exception e) {
            System.err.println(" Tiger API connection failed: " + e.getMessage());
            System.err.println("   Убедитесь, что Tiger Trade запущен и локальный сервер сигналов включен");
            return false;
        }
    }

    /**
     * Отправить команду setLinkSymbol
     */
    public void sendLinkSymbol(String exchange, String market, String symbol) {
        if (!isConnected()) {
            System.err.println(" Not connected to Tiger API");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(new LinkSymbolCommand(
                    "setLinkSymbol",
                    exchange.toUpperCase(),
                    market.toUpperCase(),
                    symbol.toUpperCase(),
                    linkGroup
            ));

            session.getBasicRemote().sendText(json);
            System.out.println("📤 Tiger: " + symbol + " → группа " + linkGroup);

        } catch (IOException e) {
            System.err.println(" Failed to send to Tiger: " + e.getMessage());
        }
    }

    /**
     * Отключиться от Tiger API
     */
    public void disconnect() {
        connected.set(false);
        if (session != null && session.isOpen()) {
            try {
                session.close();
                System.out.println("🔌 Tiger API disconnected");
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Проверка подключения
     */
    public boolean isConnected() {
        return connected.get() && session != null && session.isOpen();
    }

    /**
     * Команда для Tiger API
     */
    private static class LinkSymbolCommand {
        public String type;
        public String e;
        public String m;
        public String symbol;
        public String linkGroup;

        public LinkSymbolCommand(String type, String e, String m, String symbol, String linkGroup) {
            this.type = type;
            this.e = e;
            this.m = m;
            this.symbol = symbol;
            this.linkGroup = linkGroup;
        }
    }

    /**
     * Внутренний класс - WebSocket endpoint
     */
    @ClientEndpoint
    private class TigerEndpoint extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig endpointConfig) {
            TigerWebSocketClient.this.session = session;
            connected.set(true);
            System.out.println(" Connected to Tiger API (ws://localhost:7819)");
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            connected.set(false);
            System.out.println(" Tiger API closed: " + closeReason.getReasonPhrase());
        }

        @Override
        public void onError(Session session, Throwable throwable) {
            System.err.println(" Tiger API error: " + throwable.getMessage());
        }
    }
}
