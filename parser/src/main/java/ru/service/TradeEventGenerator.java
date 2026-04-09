package ru.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import ru.config.GeneratorConfig;
import ru.dto.TradeEvent;
import ru.publishers.TradesGenerator;
import ru.tinkoff.kora.common.Component;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TradeEventGenerator {

    private final TradesGenerator tradesGenerator;
    private final GeneratorConfig config;

    private final AtomicBoolean generating = new AtomicBoolean(false);
    private final AtomicLong generatedCount = new AtomicLong(0);

    public TradeEventGenerator(TradesGenerator tradesGenerator, GeneratorConfig config) {
        this.tradesGenerator = tradesGenerator;
        this.config = config;
    }

    public GenerationResult generate(int count) {
        if (generating.compareAndSet(false, true)) {
            try {
                return doGenerate(count);
            } finally {
                generating.set(false);
            }
        } else {
            return new GenerationResult(0, 0, "generation_already_in_progress");
        }
    }

    private GenerationResult doGenerate(int count) {
        Random random = new Random(42);
        long price = config.startPrice();
        generatedCount.set(0);

        long baseTimestampNs = System.nanoTime();
        long tradeId = 0;

        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        long totalVolume = 0;

        for (int i = 0; i < count; i++) {
            // Спайк каждые spikeInterval тиков
            if (config.spikeInterval() > 0 && i % config.spikeInterval() == 0 && i > 0) {
                double spikeDir = (i / config.spikeInterval()) % 2 == 0 ? config.spikeAmplitude() : -config.spikeAmplitude();
                price = (long) (price * (1 + spikeDir));
            }

            // Random walk: price(t) = price(t-1) * (1 + trend + randomNoise)
            double noise = (random.nextDouble() - 0.5) * 2 * config.volatility();
            double change = config.trend() + noise;
            price = (long) (price * (1 + change));
            price = Math.max(price, 1);

            // Объём с шумом
            long volume = Math.max(1, config.volumeBase() + (long) ((random.nextDouble() - 0.5) * config.volumeBase() * 0.6));

            // Side: 0 = sell, 1 = buy
            int side = change >= 0 ? 1 : 0;

            // isBuyerMaker — случайный
            int isBuyerMaker = random.nextInt(2);

            TradeEvent event = new TradeEvent(
                    "binance",              // exchange
                    "futures",              // market
                    "btcusdt",              // symbol
                    tradeId + i,            // id
                    baseTimestampNs + i * 1_000_000L,  // timestampNs (1ms шаг)
                    tradeId + i,            // firstTradeId
                    tradeId + i + 1,        // lastTradeId
                    price,                  // price
                    volume,                 // amount
                    price * volume / 1_000_000,  // quoteQty
                    side,
                    isBuyerMaker
            );

            minPrice = Math.min(minPrice, price);
            maxPrice = Math.max(maxPrice, price);
            totalVolume += volume;

            ProducerRecord<String, TradeEvent> record =
                    new ProducerRecord<>("trades-topic", "btcusdt", event);

            tradesGenerator.send(record);
            generatedCount.incrementAndGet();
        }

        return new GenerationResult(
                generatedCount.get(),
                price,
                (long) minPrice,
                (long) maxPrice,
                totalVolume,
                "ok"
        );
    }

    public boolean isGenerating() {
        return generating.get();
    }

    public long getGeneratedCount() {
        return generatedCount.get();
    }

    public record GenerationResult(
            long count,
            long lastPrice,
            long minPrice,
            long maxPrice,
            long totalVolume,
            String status
    ) {
        public GenerationResult(long count, long lastPrice, String status) {
            this(count, lastPrice, 0, 0, 0, status);
        }
    }
}
