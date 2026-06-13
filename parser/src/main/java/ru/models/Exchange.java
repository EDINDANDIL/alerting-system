package ru.models;

import org.apache.kafka.clients.producer.ProducerRecord;
import ru.dto.TradeTick;
import ru.publishers.TradePublisher;
import ru.serde.TradeEventBinaryEncoder;
import ru.tinkoff.kora.common.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Exchange {
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final TradePublisher publisher;

    public Exchange(TradePublisher publisher) {
        this.publisher = publisher;
    }

    private final Map<String, Long> tickSizes = new ConcurrentHashMap<>();

    public void registerSymbol(String symbol, long tickSize) {
        tickSizes.put(symbol, tickSize);
    }

    public long getTickSize(String symbol) {
        return tickSizes.getOrDefault(symbol, 1_000_000L); // Дефолтный шаг цены $0.01 (1_000_000 в масштабе 10^8)
    }

    public void order(Order order) {
        OrderBook orderBook = orderBooks.computeIfAbsent(
                order.getSymbol(),
                sym -> new OrderBook(sym, getTickSize(sym))
        );
        List<TradeTick> trades = orderBook.order(order);

        // TODO делегировать задачу
        if (!trades.isEmpty()) {
            trades.forEach(e -> publisher.send(
            new ProducerRecord<>(
                    "trades-topic",
                    order.getSymbol(),
                    TradeEventBinaryEncoder.encode(e)
            )
            ));
        }
    }

    public void cancel(Order order) {
        OrderBook orderBook = orderBooks.get(order.getSymbol());
        if (orderBook == null) return;
        orderBook.cancel(order);
    }

    public long getMarketPrice(String symbol) {
        OrderBook book = orderBooks.get(symbol);
        if (book == null) return 0;

        long bestBid = book.getBestBid();
        long bestAsk = book.getBestAsk();
        return (bestBid == 0 || bestAsk == 0) ? 0 : (bestBid + bestAsk) / 2;
    }

    public long getBestBid(String symbol) {
        OrderBook book= orderBooks.get(symbol);
        return book == null ? 0 : book.getBestBid();
    }

    public long getBestAsk(String symbol) {
        OrderBook book= orderBooks.get(symbol);
        return book == null ? 0 : book.getBestAsk();
    }
}