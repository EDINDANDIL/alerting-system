package ru.alertcli.commands;

import ru.alertcli.client.AlertWebSocketClient;
import ru.alertcli.client.FilterHttpClient;
import ru.alertcli.client.TigerWebSocketClient;

/**
 * Контекст для обмена клиентами между командами.
 */
public class CommandContext {
    private static FilterHttpClient filterClient;
    private static AlertWebSocketClient wsClient;
    private static TigerWebSocketClient tigerClient;
    private static boolean autoTigerEnabled = false;

    public static void init(String filterServiceUrl) {
        filterClient = new FilterHttpClient(filterServiceUrl);
        wsClient = new AlertWebSocketClient();
        tigerClient = new TigerWebSocketClient("A"); // TODO группа должна настраиваться, сейчас по умолчанию
    }

    public static FilterHttpClient getFilterClient() {
        return filterClient;
    }

    public static AlertWebSocketClient getWsClient() {
        return wsClient;
    }

    public static TigerWebSocketClient getTigerClient() {
        return tigerClient;
    }

    public static boolean isAutoTigerEnabled() {
        return autoTigerEnabled;
    }

    public static void setAutoTigerEnabled(boolean enabled) {
        autoTigerEnabled = enabled;
    }
}
