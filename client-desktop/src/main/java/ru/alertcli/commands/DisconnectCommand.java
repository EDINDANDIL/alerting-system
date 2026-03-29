package ru.alertcli.commands;

import picocli.CommandLine;
import ru.alertcli.client.AlertWebSocketClient;

@CommandLine.Command(name = "disconnect", description = "Отключиться от WebSocket")
public class DisconnectCommand implements Runnable {

    @Override
    public void run() {
        AlertWebSocketClient client = CommandContext.getWsClient();

        if (!client.isConnected()) {
            System.out.println("Not connected to WebSocket");
            return;
        }

        client.disconnect();
    }
}
