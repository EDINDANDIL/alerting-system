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

    private static final int MESSAGE_SIZE = 16;
    private static final int PRICE_OFFSET = 0;
    private static final int TIMESTAMP_OFFSET = 8;

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<TradeTick> out) {
        byte[] key = record.key();
        byte[] value = record.value();

        if (value == null) return;
        if (value.length != MESSAGE_SIZE) return;

        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        String symbol = key == null ? "" : new String(key, StandardCharsets.UTF_8);
        long price = buffer.getLong(PRICE_OFFSET);
        long timestampNs = buffer.getLong(TIMESTAMP_OFFSET);

        out.collect(new TradeTick(symbol, price, timestampNs));
    }

    @Override
    public TypeInformation<TradeTick> getProducedType() {
        return TypeInformation.of(TradeTick.class);
    }
}
