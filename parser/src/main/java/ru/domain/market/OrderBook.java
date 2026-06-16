package ru.domain.market;

import ru.dto.MarketLevel;
import ru.dto.OrderBookSnapshot;
import ru.dto.TradeTick;

import java.util.*;

public class OrderBook {

    private final String name;

    private final NavigableMap<Long, Map<Long, Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<Long, Map<Long, Order>> asks = new TreeMap<>();

    public OrderBook(String name) {this.name = name;}

    public String name() {return name;}

    public synchronized List<TradeTick> order(Order order) {
        List<TradeTick> trades = new ArrayList<>();

        if (order.getSide() == Side.BUY) {
            match(order, asks, trades);
            if (!order.isFilled() && order.getType() == Type.LIMIT) {
                bids.computeIfAbsent(order.getPrice(), _ -> new LinkedHashMap<>()).put(order.getId(),order);
            }
        } else {
            match(order, bids, trades);
            if (!order.isFilled() && order.getType() == Type.LIMIT) {
                asks.computeIfAbsent(order.getPrice(), _ -> new LinkedHashMap<>()).put(order.getId(),order);
            }
        }
        return trades;
    }

    private void match(Order order, NavigableMap<Long, Map<Long, Order>> oppositeBook, List<TradeTick> trades) {

        while (!order.isFilled() && !oppositeBook.isEmpty()) {
            Map.Entry<Long, Map<Long, Order>> bestEntry = oppositeBook.firstEntry();
            long bestPrice = bestEntry.getKey();

            if (order.getType() == Type.LIMIT) {
                if (order.getSide() == Side.BUY && order.getPrice() < bestPrice) break;
                if (order.getSide() == Side.SELL && order.getPrice() > bestPrice) break;
            }

            Map<Long, Order> queueWithOffers = bestEntry.getValue(); // LinkedHashMap с ордерами!
            while (!order.isFilled() && !queueWithOffers.isEmpty()) {
                Map.Entry<Long, Order> entry = queueWithOffers.entrySet().iterator().next();
                Order oppositeOrder = entry.getValue();
                long matchQty = Math.min(order.getCount(), oppositeOrder.getCount());

                order.reduceCount(matchQty);
                oppositeOrder.reduceCount(matchQty);

                order.getTrader().onOrderFilled(order, bestPrice, matchQty);
                oppositeOrder.getTrader().onOrderFilled(oppositeOrder, bestPrice, matchQty);

                trades.add(new TradeTick(bestPrice, System.nanoTime()));

                if (oppositeOrder.isFilled()) queueWithOffers.remove(entry.getKey());
            }
            if (queueWithOffers.isEmpty()) oppositeBook.remove(bestPrice);
        }
    }

    public synchronized void cancel(Order order) {
        if (order.getSide() == Side.BUY) {
            var map = bids.get(order.getPrice());
            if (map != null) map.remove(order.getId());
            if (map != null && map.isEmpty()) bids.remove(order.getPrice());
        }
        if (order.getSide() == Side.SELL) {
            var map = asks.get(order.getPrice());
            if (map != null) map.remove(order.getId());
            if (map != null && map.isEmpty()) asks.remove(order.getPrice());
        }
    }

    public synchronized long getBestBid() {return bids.isEmpty() ? 0 : bids.firstKey();}

    public synchronized long getBestAsk() {return asks.isEmpty() ? 0 : asks.firstKey();}

    public synchronized OrderBookSnapshot getSnapshot(int depth) {
        List<MarketLevel> bidsSnapshot = bids.entrySet().stream()
        .limit(depth)
        .map(e -> new MarketLevel(
        e.getKey(),
        e.getValue().values().stream().mapToLong(Order::getCount).sum()
        )).toList();

        List<MarketLevel> asksSnapshot = asks.entrySet().stream()
        .limit(depth)
        .map(e -> new MarketLevel(
        e.getKey(),
        e.getValue().values().stream().mapToLong(Order::getCount).sum()
        ))
        .toList();

        return new OrderBookSnapshot(name, bidsSnapshot, asksSnapshot);
    }
}
