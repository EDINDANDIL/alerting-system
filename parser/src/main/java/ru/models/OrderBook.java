package ru.models;

import ru.dto.TradeTick;

import java.util.*;

public class OrderBook {

    private final String name;

    private final NavigableMap<Long, Map<Long ,Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<Long, Map<Long, Order>> asks = new TreeMap<>();

    public OrderBook(String name) {this.name = name;}

    public String name() {return name;}

    public List<TradeTick> order(Order order) {
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
        long timestamp = System.nanoTime();

        while (!order.isFilled() && !oppositeBook.isEmpty()) {
            Map.Entry<Long, Map<Long, Order>> bestEntry = oppositeBook.firstEntry();
            long bestPrice = bestEntry.getKey();

            if (order.getType() == Type.LIMIT) {
                if (order.getSide() == Side.BUY && order.getPrice() < bestPrice) break;
                if (order.getSide() == Side.SELL && order.getPrice() > bestPrice) break;
            }

            Map<Long, Order> map = bestEntry.getValue();
            while (!order.isFilled() && !map.isEmpty()) {
                Map.Entry<Long, Order> entry = map.entrySet().iterator().next();
                Order oppositeOrder = entry.getValue();
                long matchQty = Math.min(order.getCount(), oppositeOrder.getCount());

                order.reduceCount(matchQty);
                oppositeOrder.reduceCount(matchQty);

                trades.add(new TradeTick(bestPrice, timestamp));

                if (oppositeOrder.isFilled()) map.remove(entry.getKey());
            }
            if (map.isEmpty()) oppositeBook.remove(bestPrice);
        }
    }

    public void cancel(Order order) {
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

    public long getBestBid() {return bids.isEmpty() ? 0 : bids.firstKey();}

    public long getBestAsk() {return asks.isEmpty() ? 0 : asks.firstKey();}
}