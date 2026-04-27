package ru.flink.serde;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.apache.flink.util.Collector;
import ru.flink.model.TradeTick;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TradeTickKafkaDeserializerTest {

    private final TradeTickKafkaDeserializer deserializer = new TradeTickKafkaDeserializer();

    @Test
    void deserialize_validRecord_emitsTradeTick() {
        List<TradeTick> out = new ArrayList<>();

        deserializer.deserialize(record("BST", payload(100L, 123_456L)), collector(out));

        assertEquals(1, out.size());
        assertEquals(new TradeTick("BST", 100L, 123_456L), out.getFirst());
    }

    @Test
    void deserialize_emptyKey_skipsRecord() {
        List<TradeTick> out = new ArrayList<>();

        deserializer.deserialize(record("", payload(100L, 123_456L)), collector(out));

        assertTrue(out.isEmpty());
    }

    @Test
    void deserialize_invalidPayloadSize_skipsRecord() {
        List<TradeTick> out = new ArrayList<>();

        deserializer.deserialize(record("BST", new byte[35]), collector(out));

        assertTrue(out.isEmpty());
    }

    @Test
    void deserialize_unsupportedHeader_skipsRecord() {
        byte[] payload = payload(100L, 123_456L);
        ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).putInt(0, 999);
        List<TradeTick> out = new ArrayList<>();

        deserializer.deserialize(record("BST", payload), collector(out));

        assertTrue(out.isEmpty());
    }

    @Test
    void getProducedType_returnsTradeTickType() {
        assertEquals(TradeTick.class, deserializer.getProducedType().getTypeClass());
    }

    private static ConsumerRecord<byte[], byte[]> record(String key, byte[] value) {
        return new ConsumerRecord<>(
                "trades-topic",
                0,
                0L,
                key.getBytes(StandardCharsets.UTF_8),
                value
        );
    }

    private static Collector<TradeTick> collector(List<TradeTick> out) {
        return new Collector<>() {
            @Override
            public void collect(TradeTick record) {
                out.add(record);
            }

            @Override
            public void close() {
            }
        };
    }

    private static byte[] payload(long price, long timestampNs) {
        ByteBuffer buffer = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(1);
        buffer.putInt(2);
        buffer.putInt(16);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putLong(price);
        buffer.putLong(timestampNs);
        return buffer.array();
    }
}
