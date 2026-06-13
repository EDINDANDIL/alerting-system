package ru.models;

import ru.tinkoff.kora.common.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class Simulation {
    private final Exchange exchange;
    private final List<Trader> traders;
    private final ExecutorService executorService;
    private final int threadCount;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private long currentTick = 0;

    public Simulation(Exchange exchange, List<Trader> traders, int threadCount) {
        this.exchange = exchange;
        this.traders = traders;
        this.threadCount = threadCount;
        this.executorService = Executors.newFixedThreadPool(threadCount);
    }

    public void runTickParallel() throws InterruptedException {
        // Делим трейдеров на группы (по числу потоков)
        int chunkSize = (int) Math.ceil((double) traders.size() / threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < traders.size(); i += chunkSize) {
            final int start = i;
            final int end = Math.min(i + chunkSize, traders.size());

            tasks.add(() -> {
                for (int j = start; j < end; j++) {
                    Trader trader = traders.get(j);
                    List<Order> orders = trader.tick(exchange, currentTick);
                    // Отправляем сгенерированные ордера на общую биржу
                    orders.forEach(exchange::order);
                }
                return null;
            });
        }

        // Запускаем обсчет тика параллельно и ждем завершения всех задач
        executorService.invokeAll(tasks);
        currentTick++;
    }
}