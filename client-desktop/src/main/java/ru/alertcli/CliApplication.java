package ru.alertcli;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import ru.alertcli.client.AlertWebSocketClient;
import ru.alertcli.commands.*;
import ru.alertcli.model.AlertMessage;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Консольный CLI клиент для alerting-system.
 * <p>
 * Команды:
 * - filter create --user-id N --exchange binance --market futures --direction UP --percent 2
 * - filter delete --user-id N --exchange binance --market futures --direction UP --percent 2
 * - connect --user-id N --ws-port 7818
 * - disconnect
 * - status
 * - exit
 */
public class CliApplication {

    private final CommandLine commandLine;
    private final AlertWebSocketClient wsClient;
    private final AtomicBoolean running;
    private Thread alertListenerThread;

    public CliApplication() {
        this.running = new AtomicBoolean(true);
        this.wsClient = new AlertWebSocketClient();

        // Инициализация контекста
        CommandContext.init("http://localhost:8081");

        // Настройка Picocli
        this.commandLine = new CommandLine(new MainCommand());
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    }

    public static void main(String[] args) {
        printBanner();
        new CliApplication().run();
    }

    private void run() {
        // Запуск фонового потока для прослушивания алертов
        startAlertListener();

        // Настройка JLine терминала
        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .jansi(true) // Включить ANSI цвета на Windows
                .build()) {

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            System.out.println("Type 'help' for available commands\n");

            // REPL цикл
            while (running.get()) {
                try {
                    String line = reader.readLine("alert-cli> ");

                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    if ("help".equalsIgnoreCase(line.trim())) {
                        printHelp();
                        continue;
                    }

                    // Выполнение команды через Picocli
                    commandLine.execute(line.split("\\s+"));

                } catch (UserInterruptException e) {
                    // Ctrl+C
                    System.out.println("\n👋 Goodbye!");
                    running.set(false);
                    break;
                } catch (EndOfFileException e) {
                    // Ctrl+D
                    running.set(false);
                    break;
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }

            // Cleanup
            shutdown();

        } catch (Exception e) {
            System.err.println("Terminal error: " + e.getMessage());
        }
    }

    private void startAlertListener() {
        alertListenerThread = new Thread(() -> {
            while (running.get()) {
                AlertMessage alert = wsClient.pollAlert();
                if (alert != null) {
                    // Вывод алерта
                    System.out.println("\n🔔 ALERT: " + alert);

                    // Автоматическая отправка в Tiger API
                    if (CommandContext.isAutoTigerEnabled() && CommandContext.getTigerClient().isConnected()) {
                        CommandContext.getTigerClient().sendLinkSymbol(
                                alert.getExchange(),
                                alert.getMarket(),
                                alert.getSymbol()
                        );
                    }

                    System.out.print("alert-cli> ");
                    System.out.flush();
                }
            }
        }, "AlertListener");
        alertListenerThread.setDaemon(true);
        alertListenerThread.start();
    }

    private void shutdown() {
        running.set(false);
        wsClient.disconnect();
        CommandContext.getTigerClient().disconnect();
        if (alertListenerThread != null) {
            alertListenerThread.interrupt();
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                              ║");
        System.out.println("║            ████████   ████████   ███████    ████████   ███████               ║");
        System.out.println("║               ██         ██      ██    ██   ███       ██                     ║");
        System.out.println("║               ██         ██      ███████    ████████  ██    ███              ║");
        System.out.println("║               ██         ██      ██   ██    ███       ██     ██              ║");
        System.out.println("║               ██      ████████   ██    ██   ████████   ███████               ║");
        System.out.println("║                                                                              ║");
        System.out.println("║                                                                              ║");
        System.out.println("║                                 T I R E G                                    ║");
        System.out.println("║                 Tiger Auto-Link Client для alerting-system                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void printHelp() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                            Доступные команды                             ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ filter create --user-id N --exchange binance --market futures           ║");
        System.out.println("║               --direction UP|DOWN --percent 2.0                          ║");
        System.out.println("║                 Создать фильтр IMPULSE                                   ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ filter delete --user-id N --exchange binance --market futures           ║");
        System.out.println("║                 --direction UP|DOWN --percent 2.0                        ║");
        System.out.println("║                 Удалить фильтр (те же параметры)                         ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ connect --user-id N [--ws-port 7818] [--auto-tiger]                     ║");
        System.out.println("║                 Подключиться к WebSocket alert-service                   ║");
        System.out.println("║                 --auto-tiger: автоматически отправлять в Tiger API       ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ disconnect          Отключиться от WebSocket                             ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ status              Показать статус подключений                          ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ help                Эта справка                                          ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ exit, quit, q       Выйти из приложения                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Примеры:");
        System.out.println("  filter create --user-id 1 --exchange binance --market futures --direction UP --percent 2");
        System.out.println("  connect --user-id 1 --ws-port 7818 --auto-tiger");
        System.out.println();
    }

    @CommandLine.Command(
        name = "main",
        subcommands = {
            FilterCommand.class,
            ConnectCommand.class,
            DisconnectCommand.class,
            StatusCommand.class,
            ExitCommand.class
        }
    )
    static class MainCommand implements Runnable {
        @Override
        public void run() {
            System.out.println("Type 'help' for available commands");
        }
    }
}
