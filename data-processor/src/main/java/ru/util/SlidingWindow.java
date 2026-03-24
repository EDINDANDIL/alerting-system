package ru.util;

import ru.core.trades.TradePoint;

import java.util.ArrayDeque;
import java.util.Deque;

public class SlidingWindow {

    private final long windowLengthNs;
    private final Deque<TradePoint> minDeque = new ArrayDeque<>();
    private final Deque<TradePoint> maxDeque = new ArrayDeque<>();

    public SlidingWindow(long windowLengthNs) {
        this.windowLengthNs = windowLengthNs;
    }

    public synchronized void add(TradePoint newPoint) {
        long cutoff = newPoint.timestampNs() - windowLengthNs;

        while (!minDeque.isEmpty() && minDeque.getFirst().timestampNs() < cutoff) {
            minDeque.removeFirst();
        }
        while (!maxDeque.isEmpty() && maxDeque.getFirst().timestampNs() < cutoff) {
            maxDeque.removeFirst();
        }

        while (!minDeque.isEmpty() && minDeque.getLast().priceRaw() >= newPoint.priceRaw()) {
            minDeque.removeLast();
        }
        minDeque.addLast(newPoint);

        while (!maxDeque.isEmpty() && maxDeque.getLast().priceRaw() <= newPoint.priceRaw()) {
            maxDeque.removeLast();
        }
        maxDeque.addLast(newPoint);
    }

    public synchronized double getCurrentImpulsePercent() {
        if (minDeque.isEmpty() || maxDeque.isEmpty()) {
            return 0.0;
        }

        TradePoint min = minDeque.getFirst();
        TradePoint max = maxDeque.getFirst();

        if (min.priceRaw() == 0L) {
            return 0.0;
        }

        double abs = (double) (max.priceRaw() - min.priceRaw()) / min.priceRaw();

        if (max.timestampNs() > min.timestampNs()) return abs;   // UP
        if (max.timestampNs() < min.timestampNs()) return -abs;  // DOWN
        return 0.0;
    }
}