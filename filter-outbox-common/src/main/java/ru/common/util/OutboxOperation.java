package ru.common.util;

public enum OutboxOperation {
    CREATE(0),
    DELETE(1),
    SUBSCRIBE(2),
    UNSUBSCRIBE(3);

    private final int code;

    OutboxOperation(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static OutboxOperation fromCode(int code) {
        return switch (code) {
            case 0 -> CREATE;
            case 1 -> DELETE;
            case 2 -> SUBSCRIBE;
            case 3 -> UNSUBSCRIBE;
            default -> throw new IllegalArgumentException("Unknown status code: " + code);
        };
    }
}
