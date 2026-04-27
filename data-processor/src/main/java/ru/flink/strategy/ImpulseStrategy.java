package ru.flink.strategy;

import ru.common.util.Direction;
import ru.flink.model.RuntimeFilter;
import ru.flink.state.PriceWindow;

public final class ImpulseStrategy {

    public boolean trigger(PriceWindow window, RuntimeFilter filter) {

        long min = window.min();
        long max = window.max();

        if (min == 0L) return false;

        boolean isUp = window.isUpMove();
        boolean amplitude = max * 100L >= min * (100L + filter.payload().percent());

        Direction direction = filter.payload().direction();

        return switch (direction) {
            case UP -> isUp && amplitude;
            case DOWN -> !isUp && amplitude;
            case BOTH -> amplitude;
        };
    }
}