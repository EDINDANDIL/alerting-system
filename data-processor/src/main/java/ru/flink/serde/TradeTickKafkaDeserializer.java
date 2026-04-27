package ru.flink.serde;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import ru.flink.model.TradeTick;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class TradeTickKafkaDeserializer
        implements KafkaRecordDeserializationSchema<TradeTick> {

    private static final int MESSAGE_SIZE = 36;

    private static final int EVENT_TYPE_OFFSET = 0;
    private static final int VERSION_OFFSET = 4;
    private static final int PAYLOAD_SIZE_OFFSET = 8;
    private static final int PRICE_OFFSET = 20;
    private static final int TIMESTAMP_OFFSET = 28;

    private static final int EXPECTED_EVENT_TYPE = 1;
    private static final int EXPECTED_VERSION = 2;
    private static final int EXPECTED_PAYLOAD_SIZE = 16;

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<TradeTick> out) {
        byte[] key = record.key();
        byte[] value = record.value();

        if (key == null || key.length == 0) return;
        if (value == null || value.length != MESSAGE_SIZE) return;

        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

        int eventType = buffer.getInt(EVENT_TYPE_OFFSET);
        int version = buffer.getInt(VERSION_OFFSET);
        int payloadSize = buffer.getInt(PAYLOAD_SIZE_OFFSET);

        if (eventType != EXPECTED_EVENT_TYPE
            || version != EXPECTED_VERSION
            || payloadSize != EXPECTED_PAYLOAD_SIZE) {
            return;
        }

        String symbol = new String(key, StandardCharsets.UTF_8);
        long price = buffer.getLong(PRICE_OFFSET);
        long timestampNs = buffer.getLong(TIMESTAMP_OFFSET);

        out.collect(new TradeTick(symbol, price, timestampNs));
    }

    @Override
    public TypeInformation<TradeTick> getProducedType() {
        return TypeInformation.of(TradeTick.class);
    }
}