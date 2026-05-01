package ru.flink.serde;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.apache.flink.util.Collector;
import ru.flink.model.KeyedTradeTick;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeyedTradeTickKafkaDeserializerTest {

    private final TradeTickKafkaDeserializer deserializer = new TradeTickKafkaDeserializer();

    @Test
    void deserialize_validRecord_emitsTradeTick() {
        List<KeyedTradeTick> out = new ArrayList<>();

        deserializer.deserialize(record("BST", payload(100L, 123_456L)), collector(out));

        assertEquals(1, out.size());
        assertEquals(new KeyedTradeTick("BST", 100L, 123_456L), out.getFirst());
    }

    @Test
    void deserialize_emptyKey_emitsTradeTickWithEmptySymbol() {
        List<KeyedTradeTick> out = new ArrayList<>();

        deserializer.deserialize(record("", payload(100L, 123_456L)), collector(out));

        assertEquals(1, out.size());
        assertEquals(new KeyedTradeTick("", 100L, 123_456L), out.getFirst());
    }

    @Test
    void deserialize_nullKey_emitsTradeTickWithEmptySymbol() {
        List<KeyedTradeTick> out = new ArrayList<>();

        deserializer.deserialize(record(null, payload(100L, 123_456L)), collector(out));

        assertEquals(1, out.size());
        assertEquals(new KeyedTradeTick("", 100L, 123_456L), out.getFirst());
    }

    @Test
    void deserialize_invalidPayloadSize_skipsRecord() {
        List<KeyedTradeTick> out = new ArrayList<>();

        deserializer.deserialize(record("BST", new byte[35]), collector(out));

        assertTrue(out.isEmpty());
    }

    @Test
    void deserialize_headeredPayload_skipsRecord() {
        byte[] payload = headeredPayload(100L, 123_456L);
        List<KeyedTradeTick> out = new ArrayList<>();

        deserializer.deserialize(record("BST", payload), collector(out));

        assertTrue(out.isEmpty());
    }

    @Test
    void getProducedType_returnsTradeTickType() {
        assertEquals(KeyedTradeTick.class, deserializer.getProducedType().getTypeClass());
    }

    private static ConsumerRecord<byte[], byte[]> record(String key, byte[] value) {
        return new ConsumerRecord<>(
                "trades-topic",
                0,
                0L,
                key == null ? null : key.getBytes(StandardCharsets.UTF_8),
                value
        );
    }

    private static Collector<KeyedTradeTick> collector(List<KeyedTradeTick> out) {
        return new Collector<>() {
            @Override
            public void collect(KeyedTradeTick record) {
                out.add(record);
            }

            @Override
            public void close() {
            }
        };
    }

    private static byte[] payload(long price, long timestampNs) {
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(price);
        buffer.putLong(timestampNs);
        return buffer.array();
    }

    private static byte[] headeredPayload(long price, long timestampNs) {
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
