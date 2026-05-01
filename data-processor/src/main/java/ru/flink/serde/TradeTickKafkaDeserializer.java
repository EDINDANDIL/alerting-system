package ru.flink.serde;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.flink.model.KeyedTradeTick;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class TradeTickKafkaDeserializer
        implements KafkaRecordDeserializationSchema<KeyedTradeTick> {

    private static final Logger log = LoggerFactory.getLogger(TradeTickKafkaDeserializer.class);
    private static final int MESSAGE_SIZE = 16;
    private static final int PRICE_OFFSET = 0;
    private static final int TIMESTAMP_OFFSET = 8;

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<KeyedTradeTick> out) {
        byte[] key = record.key();
        byte[] value = record.value();

        if (value == null) {
            log.warn("trade skipped: null value topic={} partition={} offset={} key={}",
                    record.topic(), record.partition(), record.offset(), decodeKey(key));
            return;
        }
        if (value.length != MESSAGE_SIZE) {
            log.warn("trade skipped: invalid payload size={} expected={} topic={} partition={} offset={} key={} firstBytes={}",
                    value.length, MESSAGE_SIZE, record.topic(), record.partition(), record.offset(),
                    decodeKey(key), hexPrefix(value));
            return;
        }

        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        String symbol = decodeKey(key);
        long price = buffer.getLong(PRICE_OFFSET);
        long timestampNs = buffer.getLong(TIMESTAMP_OFFSET);

        log.debug("trade decoded: symbol={} price={} timestampNs={} topic={} partition={} offset={}",
                symbol, price, timestampNs, record.topic(), record.partition(), record.offset());
        out.collect(new KeyedTradeTick(symbol, price, timestampNs));
    }

    @Override
    public TypeInformation<KeyedTradeTick> getProducedType() {
        return TypeInformation.of(KeyedTradeTick.class);
    }

    private static String decodeKey(byte[] key) {
        return key == null ? "" : new String(key, StandardCharsets.UTF_8);
    }

    private static String hexPrefix(byte[] value) {
        int limit = Math.min(value.length, MESSAGE_SIZE);
        StringBuilder result = new StringBuilder(limit * 3);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(String.format("%02x", value[i] & 0xff));
        }
        if (value.length > limit) {
            result.append(" ...");
        }
        return result.toString();
    }
}
