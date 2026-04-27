package ru.serde;

import ru.dto.TradeEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class TradeEventBinaryEncoder {

    private static final int MESSAGE_SIZE = 36;
    private static final int EVENT_TYPE = 1;
    private static final int VERSION = 2;
    private static final int PAYLOAD_SIZE = 16;
    private static final int OPERATION_RAW = 0;
    private static final int OBJECT_RAW = 0;

    private TradeEventBinaryEncoder() {
    }

    public static byte[] encode(TradeEvent event) {
        ByteBuffer buffer = ByteBuffer.allocate(MESSAGE_SIZE).order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(EVENT_TYPE);
        buffer.putInt(VERSION);
        buffer.putInt(PAYLOAD_SIZE);
        buffer.putInt(OPERATION_RAW);
        buffer.putInt(OBJECT_RAW);
        buffer.putLong(event.price());
        buffer.putLong(event.timestampNs());

        return buffer.array();
    }
}
