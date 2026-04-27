package ru.flink;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.common.dto.OutboxCreatedEvent;
import ru.flink.model.AlertEvent;
import ru.flink.model.RuntimeFilter;
import ru.flink.model.TradeTick;
import ru.flink.operator.AlertProcessFunction;
import ru.flink.serde.AlertEventKafkaSerializer;
import ru.flink.serde.FilterEventDeserializer;
import ru.flink.serde.TradeTickKafkaDeserializer;

public final class DataProcessorJob {

    private static final String TRADES_TOPIC = "trades-topic";
    private static final String FILTER_TOPIC = "filter-topic";
    private static final Logger log = LoggerFactory.getLogger(DataProcessorJob.class);

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.enableCheckpointing(10_000);
        env.setParallelism(intEnv("FLINK_PARALLELISM", 4));

        String brokers = env("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");

        KafkaSource<TradeTick> tradesSource = KafkaSource.<TradeTick>builder()
                .setBootstrapServers(brokers)
                .setTopics(TRADES_TOPIC)
                .setGroupId("flink-data-processor-trades")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setDeserializer(new TradeTickKafkaDeserializer())
                .build();

        KafkaSource<OutboxCreatedEvent> filtersSource = KafkaSource.<OutboxCreatedEvent>builder()
                .setBootstrapServers(brokers)
                .setTopics(FILTER_TOPIC)
                .setGroupId("flink-data-processor-filters")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new FilterEventDeserializer())
                .build();

        KafkaSink<AlertEvent> sink = KafkaSink.<AlertEvent>builder()
                .setBootstrapServers(brokers)
                .setRecordSerializer(new AlertEventKafkaSerializer("alert-topic"))
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        MapStateDescriptor<Long, RuntimeFilter> filtersDescriptor =
                new MapStateDescriptor<>(
                        "filters",
                        Long.class,
                        RuntimeFilter.class
                );

        KeyedStream<TradeTick, String> trades = env
                .fromSource(tradesSource, WatermarkStrategy.noWatermarks(), "trades-source")
                .name("read-trades")
                .map(tick -> {
                    log.info("trade received: symbol={} price={} timestampNs={}",
                            tick.symbol(), tick.price(), tick.timestampNs());
                    return tick;
                })
                .keyBy(TradeTick::symbol);

        BroadcastStream<OutboxCreatedEvent> filters = env
                .fromSource(filtersSource, WatermarkStrategy.noWatermarks(), "filters-source")
                .name("read-filters")
                .map(filter -> {
                    log.info("filter accepted: {}", filter);
                    return filter;
                })
                .broadcast(filtersDescriptor);

        trades.connect(filters)
                .process(new AlertProcessFunction(filtersDescriptor))
                .sinkTo(sink);

        env.execute("alerting-data-processor");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static int intEnv(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value);
    }
}
