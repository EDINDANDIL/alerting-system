package ru.flink.state;

import ru.flink.model.TradePoint;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

public final class PriceWindow implements Serializable {

    private final long windowLengthNs;
    private final Deque<TradePoint> minDeque = new ArrayDeque<>();
    private final Deque<TradePoint> maxDeque = new ArrayDeque<>();

    public PriceWindow(long windowLengthNs) {this.windowLengthNs = windowLengthNs;}

    public void add(TradePoint point) {
        long cutoff = point.timestampNs() - windowLengthNs;

        while (!minDeque.isEmpty() && minDeque.getFirst().timestampNs() < cutoff) {
            minDeque.removeFirst();
        }

        while (!maxDeque.isEmpty() && maxDeque.getFirst().timestampNs() < cutoff) {
            maxDeque.removeFirst();
        }

        while (!minDeque.isEmpty() && minDeque.getLast().priceRaw() >= point.priceRaw()) {
            minDeque.removeLast();
        }
        minDeque.addLast(point);

        while (!maxDeque.isEmpty() && maxDeque.getLast().priceRaw() <= point.priceRaw()) {
            maxDeque.removeLast();
        }
        maxDeque.addLast(point);
    }

    public long min() {return minDeque.isEmpty() ? 0L : minDeque.getFirst().priceRaw();}

    public long max() {return maxDeque.isEmpty() ? 0L : maxDeque.getFirst().priceRaw();}

    public boolean isUpMove() {
        if (minDeque.isEmpty() || maxDeque.isEmpty()) return false;
        return maxDeque.getFirst().timestampNs() >= minDeque.getFirst().timestampNs();
    }
}