package ru.service;

import ru.dto.DepthEvent;
import ru.dto.OrderBookSnapshot;
import ru.dto.TradeEvent;
import ru.domain.market.Exchange;
import ru.domain.simulation.SimulationEngine;
import ru.dto.TradeTick;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.json.common.JsonWriter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Component
public final class MarketDataBroadcaster implements Lifecycle, Exchange.TradeListener {
    private final Exchange exchange;
    private final SimulationEngine engine;
    private final JsonWriter<DepthEvent> depthWriter;
    private final JsonWriter<TradeEvent> tradeWriter;
    private final Map<String, List<SafeStreamPublisher>> subscribers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public MarketDataBroadcaster(
            Exchange exchange,
            SimulationEngine engine,
            JsonWriter<DepthEvent> depthWriter,
            JsonWriter<TradeEvent> tradeWriter
    ) {
        this.exchange = exchange;
        this.engine = engine;
        this.depthWriter = depthWriter;
        this.tradeWriter = tradeWriter;
    }

    @Override
    public void init() {
        exchange.registerTradeListener(this);
        scheduler.scheduleAtFixedRate(this::broadcastDepth, 150, 150, TimeUnit.MILLISECONDS);
    }

    @Override
    public void release() {
        scheduler.shutdown();
        subscribers.values().forEach(list -> list.forEach(SafeStreamPublisher::close));
    }

    @Override
    public void onTrades(String symbol, List<TradeTick> trades) {
        List<SafeStreamPublisher> clients = subscribers.get(symbol);
        if (clients == null || clients.isEmpty()) return;

        long nowMs = System.currentTimeMillis();

        for (TradeTick trade : trades) {
            try {
                // Price is scaled to 10^8 in simulation, we pass it directly
                TradeEvent event = new TradeEvent("trade", symbol, trade.price(), nowMs);
                byte[] jsonBytes = tradeWriter.toByteArray(event);
                
                // Format as SSE (data: <json>\n\n)
                byte[] payloadBytes = formatSsePayload(jsonBytes);
                ByteBuffer payload = ByteBuffer.wrap(payloadBytes);

                for (SafeStreamPublisher client : clients) {
                    client.submit(payload);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public Flow.Publisher<ByteBuffer> register(String symbol) {
        List<SafeStreamPublisher> list = subscribers.computeIfAbsent(symbol, k -> new CopyOnWriteArrayList<>());

        SafeStreamPublisher publisher = new SafeStreamPublisher(pub -> {
            List<SafeStreamPublisher> subs = subscribers.get(symbol);
            if (subs != null) {
                subs.remove(pub);
                if (subs.isEmpty()) {
                    subscribers.remove(symbol);
                }
            }
        });

        list.add(publisher);
        return publisher;
    }

    private void broadcastDepth() {
        boolean isRunning = engine.isRunning();
        long currentTick = engine.getCurrentTick();

        for (Map.Entry<String, List<SafeStreamPublisher>> entry : subscribers.entrySet()) {
            String symbol = entry.getKey();
            List<SafeStreamPublisher> clients = entry.getValue();
            if (clients.isEmpty()) continue;

            try {
                OrderBookSnapshot snapshot = exchange.getOrderBookSnapshot(symbol, 15);
                DepthEvent event = new DepthEvent(
                        "depth",
                        symbol,
                        snapshot.bids(),
                        snapshot.asks(),
                        isRunning,
                        currentTick
                );

                byte[] jsonBytes = depthWriter.toByteArray(event);
                byte[] payloadBytes = formatSsePayload(jsonBytes);
                ByteBuffer payload = ByteBuffer.wrap(payloadBytes);

                for (SafeStreamPublisher client : clients) {
                    client.submit(payload);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private byte[] formatSsePayload(byte[] jsonBytes) {
        byte[] prefix = "data: ".getBytes(StandardCharsets.UTF_8);
        byte[] suffix = "\n\n".getBytes(StandardCharsets.UTF_8);
        
        byte[] sseBytes = new byte[prefix.length + jsonBytes.length + suffix.length];
        System.arraycopy(prefix, 0, sseBytes, 0, prefix.length);
        System.arraycopy(jsonBytes, 0, sseBytes, prefix.length, jsonBytes.length);
        System.arraycopy(suffix, 0, sseBytes, prefix.length + jsonBytes.length, suffix.length);
        return sseBytes;
    }
}