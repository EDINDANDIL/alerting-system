package ru.util;

import ru.core.trades.TradePoint;

import java.util.ArrayDeque;
import java.util.Deque;

public class SlidingWindow {
    private final long windowLengthUs;
    private final Deque<TradePoint> minDeque = new ArrayDeque<>();
    private final Deque<TradePoint> maxDeque = new ArrayDeque<>();

    public SlidingWindow(long windowLengthUs) {
        this.windowLengthUs = windowLengthUs;
    }

    public synchronized void add(TradePoint newPoint) {
        long cutoff = newPoint.timestampNs() - windowLengthUs;

        // 1. Удаляем устаревшие элементы из обеих очередей
        while (!minDeque.isEmpty() && minDeque.getFirst().timestampNs() < cutoff) {
            minDeque.removeFirst();
        }
        while (!maxDeque.isEmpty() && maxDeque.getFirst().timestampNs() < cutoff) {
            maxDeque.removeFirst();
        }

        // 2. Обновляем очередь минимумов
        // Удаляем с конца все элементы, у которых цена >= новой
        // (они никогда не станут минимумом, пока новая точка в окне)
        while (!minDeque.isEmpty() && minDeque.getLast().priceRaw() >= newPoint.priceRaw()) {
            minDeque.removeLast();
        }
        minDeque.addLast(newPoint);

        // 3. Обновляем очередь максимумов
        // Удаляем с конца все элементы, у которых цена <= новой
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

        // Проверяем, что элементы еще актуальны (на всякий случай)
        long currentTime = Math.max(min.timestampNs(), max.timestampNs());
        long cutoff = currentTime - windowLengthUs;

        if (min.timestampNs() < cutoff || max.timestampNs() < cutoff) {
            return 0.0;
        }

        return (double) (max.priceRaw() - min.priceRaw()) / min.priceRaw();
    }
}