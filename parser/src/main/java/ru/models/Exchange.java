package ru.models;

import org.apache.kafka.clients.producer.ProducerRecord;
import ru.dto.TradeTick;
import ru.publishers.TradePublisher;
import ru.serde.TradeEventBinaryEncoder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Exchange {
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final TradePublisher publisher;

    public Exchange(TradePublisher publisher) {
        this.publisher = publisher;
    }

    public void order(Order order) {
        OrderBook orderBook = orderBooks.computeIfAbsent(order.getSymbol(), OrderBook::new);
        List<TradeTick> trades = orderBook.order(order);

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

    public double getMarketPrice(String symbol) {
        OrderBook book = orderBooks.get(symbol);
        if (book == null) return 0;

        double bestBid = book.getBestBid();
        double bestAsk = book.getBestAsk();
        return (bestBid == 0 || bestAsk == 0) ? 0 : (bestBid + bestAsk) / 2;
    }

    public double getBestBid(String symbol) {
        OrderBook book= orderBooks.get(symbol);
        return book == null ? 0 : book.getBestBid();
    }

    public double getBestAsk(String symbol) {
        OrderBook book= orderBooks.get(symbol);
        return book == null ? 0 : book.getBestAsk();
    }
}
