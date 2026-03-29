package ru.alertcli.commands;

import picocli.CommandLine;
import ru.alertcli.client.AlertWebSocketClient;

@CommandLine.Command(name = "connect", description = "Подключиться к WebSocket alert-service")
public class ConnectCommand implements Runnable {

    @CommandLine.Option(names = "--user-id", required = true, description = "ID пользователя")
    private int userId;

    @CommandLine.Option(names = "--ws-port", defaultValue = "7818", description = "Порт WebSocket alert-service (по умолчанию 7818)")
    private int wsPort;

    @CommandLine.Option(names = "--host", defaultValue = "localhost", description = "Хост (по умолчанию localhost)")
    private String host;

    @CommandLine.Option(names = "--auto-tiger", defaultValue = "false", description = "Автоматически отправлять алерты в Tiger API")
    private boolean autoTiger;

    @CommandLine.Option(names = "--tiger-port", defaultValue = "7819", description = "Порт Tiger API (по умолчанию 7819)")
    private int tigerPort;

    @Override
    public void run() {
        System.out.println("🔌 Connecting to ws://" + host + ":" + wsPort + "/ws?userId=" + userId + "...");

        AlertWebSocketClient client = CommandContext.getWsClient();

        if (client.isConnected()) {
            System.out.println("⚠️  Already connected. Disconnect first with 'disconnect' command.");
            return;
        }

        boolean success = client.connect(host, wsPort, userId);

        if (success) {
            System.out.println("Connected to alert-service");

            if (autoTiger) {
                CommandContext.setAutoTigerEnabled(true);
                System.out.println("Auto-Tiger enabled (group A)");

                boolean tigerSuccess = CommandContext.getTigerClient().connect("localhost", tigerPort);
                if (!tigerSuccess) {
                    System.err.println("Tiger API not available. Alerts will be logged but not sent to Tiger.");
                    CommandContext.setAutoTigerEnabled(false);
                }
            }
        } else {
            System.err.println("Failed to connect. Make sure alert-service is running on port " + wsPort);
        }
    }
}
