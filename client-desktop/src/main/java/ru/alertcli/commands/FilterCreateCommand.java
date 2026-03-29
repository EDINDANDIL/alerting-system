package ru.alertcli.commands;

import picocli.CommandLine;
import ru.alertcli.client.FilterHttpClient;
import ru.alertcli.model.FilterRequest;

import java.util.Arrays;
import java.util.List;

@CommandLine.Command(name = "create", description = "Создать фильтр")
public class FilterCreateCommand implements Runnable {

    @CommandLine.ParentCommand
    private FilterCommand parent;

    @CommandLine.Option(names = "--user-id", required = true, description = "ID пользователя")
    private int userId;

    @CommandLine.Option(names = "--exchange", required = true, split = ",", description = "Биржи (через запятую): binance,bybit")
    private List<String> exchange;

    @CommandLine.Option(names = "--market", required = true, split = ",", description = "Рынки (через запятую): spot,futures")
    private List<String> market;

    @CommandLine.Option(names = "--direction", required = true, description = "Направление: UP или DOWN")
    private String direction;

    @CommandLine.Option(names = "--percent", required = true, description = "Процент изменения цены")
    private double percent;

    @CommandLine.Option(names = "--time-window", defaultValue = "60", description = "Временное окно в секундах (по умолчанию 60)")
    private int timeWindow;

    @CommandLine.Option(names = "--volume24h", defaultValue = "0", description = "Минимальный объём за 24ч (по умолчанию 0)")
    private long volume24h;

    @Override
    public void run() {
        // Валидация direction
        if (!"UP".equalsIgnoreCase(direction) && !"DOWN".equalsIgnoreCase(direction)) {
            System.err.println("direction должен быть UP или DOWN");
            return;
        }

        FilterRequest request = new FilterRequest(
                normalizeList(exchange),
                normalizeList(market),
                direction.toUpperCase(),
                percent
        );
        request.setTimeWindow(timeWindow);
        request.setVolume24h(volume24h);
        request.setBlackList(List.of());

        System.out.println("   Creating filter for user " + userId + "...");
        System.out.println("   Exchange: " + request.getExchange());
        System.out.println("   Market: " + request.getMarket());
        System.out.println("   Direction: " + request.getDirection());
        System.out.println("   Percent: " + request.getPercent() + "%");
        System.out.println("   Time window: " + request.getTimeWindow() + "s");

        FilterHttpClient client = CommandContext.getFilterClient();
        boolean success = client.createFilter(userId, request);

        if (success) {
            System.out.println("Filter created successfully");
        } else {
            System.err.println("Failed to create filter");
        }
    }

    private List<String> normalizeList(List<String> list) {
        return list.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();
    }
}
