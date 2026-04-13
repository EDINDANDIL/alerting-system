package ru.core.util;

import ru.models.domain.TradePoint;

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

    public synchronized long getMin() {
        if (minDeque.isEmpty()) return 0L;
        return minDeque.getFirst().priceRaw();
    }

    public synchronized long getMax() {
        if (maxDeque.isEmpty()) return 0L;
        return maxDeque.getFirst().priceRaw();
    }

    /** true если максимум по времени ПОЗЖЕ минимума (рост), false — падение. */
    public synchronized boolean isUpMove() {
        if (minDeque.isEmpty() || maxDeque.isEmpty()) return false;
        return maxDeque.getFirst().timestampNs() >= minDeque.getFirst().timestampNs();
    }

    /**
     * Проверить, есть ли данные в окне.
     */
    public synchronized boolean isEmpty() {
        return minDeque.isEmpty();
    }
}