package ru.domain.simulation;

import ru.config.SimulatorConfig;
import ru.domain.agent.Trader;
import ru.domain.agent.TraderType;
import ru.domain.agent.AgentRegistry;
import ru.domain.agent.AgentRegistryImpl;
import ru.domain.agent.TraderFactory;
import ru.domain.market.Exchange;
import ru.domain.market.Order;
import ru.domain.market.Side;
import ru.domain.market.Type;
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

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong currentTick = new AtomicLong(0);
    private final Map<String, Long> currentFundamentalValues = new java.util.concurrent.ConcurrentHashMap<>();
    private List<Trader> traders;
    private Future<?> simulationTask;

    private final AgentRegistry agentRegistry = new AgentRegistryImpl();
    private final SimulationContext simulationContext = new SimulationContext() {
        @Override
        public long getCurrentTick() {
            return currentTick.get();
        }

        @Override
        public AgentRegistry getRegistry() {
            return agentRegistry;
        }

        @Override
        public long getFundamentalPrice(String symbol) {
            return currentFundamentalValues.getOrDefault(symbol, 0L);
        }
    };

    public SimulationEngine(SimulatorConfig config, Exchange exchange) {
        this.config = config;
        this.exchange = exchange;
    }

    @Override
    public void init() throws Exception {}

    @Override
    public void release() throws Exception {
        stop();
        loopExecutor.shutdown();
    }

    public synchronized void start() {
        if (running.get()) return;

        running.set(true);
        currentTick.set(0);

        currentFundamentalValues.clear();
        for (String symbol : config.symbols()) {
            long startPriceUsd = config.startPrices().getOrDefault(symbol, 100L);
            currentFundamentalValues.put(symbol, startPriceUsd * 100_000_000L);
        }

        Map<String, Long> configuredTickSizes = config.tickSizes();
        for (String symbol : config.symbols()) {
            long tickSize = (configuredTickSizes != null) ? configuredTickSizes.getOrDefault(symbol, 1000L) : 1000L;
            exchange.registerSymbol(symbol, tickSize);
        }

        bootstrapInitialLiquidity();

        this.traders = createAgents();
        for (Trader trader : this.traders) {
            agentRegistry.register(trader);
        }

        simulationTask = loopExecutor.submit(this::runSimulationLoop);
    }

    public synchronized void stop() {
        running.set(false);
        if (simulationTask != null) {
            simulationTask.cancel(true);
            simulationTask = null;
        }
        if (this.traders != null) {
            for (Trader trader : this.traders) {
                agentRegistry.unregister(trader);
            }
        }
    }

    private void runSimulationLoop() {
        try {
            try {
                while (running.get()) {
                    long tick = currentTick.getAndIncrement();

                    if (config.ticks() > 0 && tick >= config.ticks()) {
                        running.set(false);
                        break;
                    }

                    for (String symbol : config.symbols()) {
                        long prevVal = currentFundamentalValues.getOrDefault(symbol, 100L * 100_000_000L);

                        // Базовая волатильность: σ = 0.001 (±0.1% за тик)
                        double change = 1.0 + (ThreadLocalRandom.current().nextGaussian() * 0.001);

                        // Редкие скачки (~1% шанс за тик) — имитация новостей/событий
                        // Амплитуда: 1-5% в случайном направлении
                        if (ThreadLocalRandom.current().nextDouble() < 0.01) {
                            double jumpSize = 0.01 + ThreadLocalRandom.current().nextDouble() * 0.04;
                            change += (ThreadLocalRandom.current().nextBoolean() ? jumpSize : -jumpSize);
                        }

                        currentFundamentalValues.put(symbol, Math.round(prevVal * change));
                    }

                    Collections.shuffle(traders);

                    for (Trader trader : traders) {
                        try {
                            List<Order> newOrders = trader.tick(exchange, simulationContext);
                            if (newOrders != null) {
                                for (Order order : newOrders) {
                                    exchange.order(order);
                                }
                            }
                        } catch (Exception e) {
                            // Suppress to keep simulator alive
                        }
                    }

                    if (config.tickDelayMs() > 0) Thread.sleep(config.tickDelayMs());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } finally {
            if (this.traders != null) {
                for (Trader trader : this.traders) {
                    agentRegistry.unregister(trader);
                }
            }
            running.set(false);
        }
    }

    private void bootstrapInitialLiquidity() {
        Trader bootstrapTrader = new Trader() {
            @Override public long id() { return 0; }
            @Override public TraderType type() { return null; }
            @Override public List<Order> tick(Exchange exchange, SimulationContext context) { return List.of(); }
            @Override public void onOrderFilled(Order order, long price, long quantity) {}
        };

        for (String symbol : config.symbols()) {
            long startPriceUsd = config.startPrices().getOrDefault(symbol, 100L);
            long initialPriceScaled = startPriceUsd * 100_000_000L;
            long tickSize = exchange.getTickSize(symbol);

            // Количество рассчитывается исходя из $50,000 стартовой ликвидности на уровень
            long quantity = Math.max(10, Math.round(50000.0 / startPriceUsd));

            // Создаём 5 уровней глубины с каждой стороны для устойчивой начальной ликвидности
            for (int level = 1; level <= 5; level++) {
                double spread = 0.002 * level; // 0.2%, 0.4%, 0.6%, 0.8%, 1.0%
                long bidPrice = Math.round((initialPriceScaled * (1.0 - spread)) / tickSize) * tickSize;
                long askPrice = Math.round((initialPriceScaled * (1.0 + spread)) / tickSize) * tickSize;
                // Дальние уровни получают больше объёма (они реже сматчатся)
                long levelQuantity = quantity * level;

                exchange.order(new Order(bootstrapTrader, Type.LIMIT, Side.BUY, symbol, bidPrice, levelQuantity));
                exchange.order(new Order(bootstrapTrader, Type.LIMIT, Side.SELL, symbol, askPrice, levelQuantity));
            }
        }
    }

    private List<Trader> createAgents() {
        List<Trader> allTraders = new ArrayList<>();

        allTraders.addAll(TraderFactory.noiseTrader(config.noiseTradersCount(), config.symbols(), config.targetUsdVolumes()));
        allTraders.addAll(TraderFactory.momentumTrader(config.momentumTradersCount(), config.symbols(), config.targetUsdVolumes()));
        allTraders.addAll(TraderFactory.marketMaker(config.marketMakersCount(), config.symbols(), config.targetUsdVolumes()));

        allTraders.addAll(TraderFactory.fundamentalTrader(
                config.fundamentalTradersCount(), config.symbols(), config.targetUsdVolumes()
        ));
        return allTraders;
    }

    public boolean isRunning() { return running.get(); }
    public long getCurrentTick() { return currentTick.get(); }
}
