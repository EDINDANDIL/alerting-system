package ru.common.util;

public enum Direction {
    UP((short) 0),
    DOWN((short) 1),
    BOTH((short) 2);

    public final short code;

    Direction(short code) {
        this.code = code;
    }

    public static Direction fromCode(short code) {
        return switch (code) {
            case 0 -> UP;
            case 1 -> DOWN;
            case 2 -> BOTH;
            default -> throw new IllegalArgumentException("Unknown direction code: " + code);
        };
    }
}