package ru.alertcli.commands;

import picocli.CommandLine;
import ru.alertcli.client.AlertWebSocketClient;
import ru.alertcli.client.TigerWebSocketClient;

@CommandLine.Command(name = "status", description = "Показать статус подключений")
public class StatusCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║           Alert CLI Status                 ║");
        System.out.println("╠════════════════════════════════════════════╣");

        // WebSocket статус
        AlertWebSocketClient wsClient = CommandContext.getWsClient();
        if (wsClient.isConnected()) {
            System.out.println("║ WebSocket:  Connected                  ║");
        } else {
            System.out.println("║ WebSocket:  Disconnected               ║");
        }

        // Tiger API статус
        TigerWebSocketClient tigerClient = CommandContext.getTigerClient();
        if (tigerClient.isConnected()) {
            System.out.println("║ Tiger API:  Connected (group A)        ║");
        } else {
            System.out.println("║ Tiger API:  Disconnected               ║");
        }

        // Auto-Tiger статус
        boolean autoTiger = CommandContext.isAutoTigerEnabled();
        System.out.println("║ Auto-Tiger: " + (autoTiger ? " ON" : " OFF") + "                          ║");

        // Filter-service статус
        System.out.println("║ Filter-service: http://localhost:8081    ║");
        System.out.println("╚════════════════════════════════════════════╝");
    }
}
