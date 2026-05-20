package ru.flink.strategy;

import ru.common.util.Direction;
import ru.flink.models.ImpulseRuntimeFilter;
import ru.flink.state.SlidingPriceWindow;

public final class ImpulseStrategy {

    public boolean trigger(SlidingPriceWindow window, ImpulseRuntimeFilter filter) {

        long min = window.min();
        long max = window.max();

        if (min == 0L) return false;

        boolean isUp = window.isUpMove();

        long percent = filter.payload().percent();

        boolean upAmplitude = max * 100L >= min * (100L + percent);
        boolean downAmplitude = min * 100L <= max * (100L - percent);

        Direction direction = filter.payload().direction();

        return switch (direction) {
            case UP -> isUp && upAmplitude;
            case DOWN -> !isUp && downAmplitude;
            case BOTH -> upAmplitude || downAmplitude;
        };
    }
}