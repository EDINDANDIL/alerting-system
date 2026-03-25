package ru.util;

import ru.core.trades.TradePoint;

import java.util.ArrayDeque;
import java.util.Deque;

public class SlidingWindow {

    private final long windowLengthNs;
    protected final Deque<TradePoint> minDeque = new ArrayDeque<>();
    protected Deque<TradePoint> maxDeque = new ArrayDeque<>();

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

    /**
     * Вычисляет текущий «импульс» в окне как подписанный коэффициент (это НЕ проценты).
     *
     * <p>Модуль импульса считается по формуле:
     * <pre>
     * (maxPriceRaw - minPriceRaw) / minPriceRaw
     * </pre>
     * где {@code maxPriceRaw} и {@code minPriceRaw} — текущие экстремумы цены в скользящем окне
     * (берутся из монотонных деков).
     *
     * <p>Знак результата:
     * <ul>
     *   <li>Положительный (UP), если максимум по времени позже минимума.</li>
     *   <li>Отрицательный (DOWN), если максимум по времени раньше минимума.</li>
     *   <li>{@code 0.0}, если данных недостаточно, окно пустое, {@code minPriceRaw == 0}
     *       или у min/max одинаковый {@code timestampNs}.</li>
     * </ul>
     *
     * <p>Единицы измерения:
     * <ul>
     *   <li>Возвращается безразмерный коэффициент: например {@code 0.10} = 10%.</li>
     *   <li>Чтобы получить проценты, умножьте результат на {@code 100}.</li>
     * </ul>
     *
     * <p>Потокобезопасность: метод синхронизирован.
     *
     * @return подписанный коэффициент импульса; {@code 0.0}, если импульс нельзя корректно посчитать
     */
    public synchronized double getCurrentImpulsePercent() {
        if (minDeque.isEmpty() || maxDeque.isEmpty()) return 0.0;

        TradePoint min = minDeque.getFirst();
        TradePoint max = maxDeque.getFirst();

        if (min.priceRaw() == 0L) return 0.0;

        double abs = (double) (max.priceRaw() - min.priceRaw()) / min.priceRaw();

        if (max.timestampNs() > min.timestampNs()) return abs;   // UP
        if (max.timestampNs() < min.timestampNs()) return -abs;  // DOWN
        return 0.0;
    }
}