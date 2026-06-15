package ru.service;

import ru.config.SimulatorConfig;
import ru.models.*;
import ru.models.factories.TraderFactory;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.common.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SimulationEngine implements Lifecycle {

    private final SimulatorConfig config;
    private final Exchange exchange;

    private final ExecutorService loopExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService agentExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong currentTick = new AtomicLong(0);
    private List<Trader> traders;
    private Future<?> simulationTask;

    public SimulationEngine(SimulatorConfig config, Exchange exchange) {
        this.config = config;
        this.exchange = exchange;
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public void release() throws Exception {
        stop();
        loopExecutor.shutdown();
        agentExecutor.shutdown();
    }

    public synchronized void start() {
        if (running.get()) return;

        running.set(true);
        currentTick.set(0);

        Map<String, Long> configuredTickSizes = config.tickSizes();
        for (String symbol : config.symbols()) {
            long tickSize = (configuredTickSizes != null) ? configuredTickSizes.getOrDefault(symbol, 1000L) : 1000L;
            exchange.registerSymbol(symbol, tickSize);
        }

        bootstrapInitialLiquidity();

        this.traders = createAgents();

        simulationTask = loopExecutor.submit(this::runSimulationLoop);
    }

    public synchronized void stop() {
        if (!running.get()) return;
        running.set(false);
        if (simulationTask != null) {
            simulationTask.cancel(true);
            simulationTask = null;
        }
    }

    private void runSimulationLoop() {
        try {
            while (running.get()) {
                long tick = currentTick.getAndIncrement();

                if (config.ticks() > 0 && tick >= config.ticks()) {
                    running.set(false);
                    break;
                }

                List<Callable<Void>> tasks = traders.stream().map(trader -> (Callable<Void>) () -> {
                    List<Order> newOrders = trader.tick(exchange, tick);
                    newOrders.forEach(exchange::order);
                    return null;
                }).toList();

                agentExecutor.invokeAll(tasks);

                if (config.tickDelayMs() > 0) Thread.sleep(config.tickDelayMs());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void bootstrapInitialLiquidity() {
        Trader bootstrapTrader = new Trader() {
            @Override public long id() { return 0; }
            @Override public List<Order> tick(Exchange exchange, long tick) { return List.of(); }
            @Override public void onOrderFilled(Order order, long price, long quantity) {}
        };

        for (String symbol : config.symbols()) {
            long startPriceUsd = config.startPrices().getOrDefault(symbol, 100L);
            long initialPriceScaled = startPriceUsd * 100_000_000L;
            long tickSize = exchange.getTickSize(symbol);

            long bidPrice = Math.round((initialPriceScaled * 0.998) / tickSize) * tickSize;
            long askPrice = Math.round((initialPriceScaled * 1.002) / tickSize) * tickSize;
            
            // Количество рассчитывается исходя из $50,000 стартовой ликвидности
            long quantity = Math.max(10, Math.round(50000.0 / startPriceUsd));

            exchange.order(new Order(bootstrapTrader, Type.LIMIT, Side.BUY, symbol, bidPrice, quantity));
            exchange.order(new Order(bootstrapTrader, Type.LIMIT, Side.SELL, symbol, askPrice, quantity));
        }
    }

    private List<Trader> createAgents() {
        List<Trader> allTraders = new ArrayList<>();

        allTraders.addAll(TraderFactory.noiseTrader(config.noiseTradersCount(), config.symbols(), config.targetUsdVolumes()));
        allTraders.addAll(TraderFactory.momentumTrader(config.momentumTradersCount(), config.symbols(), config.targetUsdVolumes()));
        allTraders.addAll(TraderFactory.marketMaker(config.marketMakersCount(), config.symbols(), config.targetUsdVolumes()));

        // Вычисляем справедливую цену Vt для фундаментальных трейдеров на основе startPrices
        Map<String, Long> targetValues = new HashMap<>();
        for (String symbol : config.symbols()) {
            long startPriceUsd = config.startPrices().getOrDefault(symbol, 100L);
            targetValues.put(symbol, startPriceUsd * 100_000_000L);
        }
        allTraders.addAll(TraderFactory.fundamentalTrader(
                config.fundamentalTradersCount(), config.symbols(), targetValues, config.targetUsdVolumes()
        ));
        return allTraders;
    }

    public boolean isRunning() { return running.get(); }
    public long getCurrentTick() { return currentTick.get(); }
}