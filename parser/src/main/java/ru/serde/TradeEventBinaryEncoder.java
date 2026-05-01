package ru.serde;

import ru.dto.TradeTick;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class TradeEventBinaryEncoder {

    private static final int MESSAGE_SIZE = 16;

    private TradeEventBinaryEncoder() {
    }

    public static byte[] encode(TradeTick event) {
        ByteBuffer buffer = ByteBuffer.allocate(MESSAGE_SIZE).order(ByteOrder.LITTLE_ENDIAN);

        buffer.putLong(event.price());
        buffer.putLong(event.timestampNs());

        return buffer.array();
    }
}
