package ru.integrationtest;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ru.Application;
import ru.services.KafkaService;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTest;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTestConfigModifier;
import ru.tinkoff.kora.test.extension.junit5.KoraConfigModification;
import ru.tinkoff.kora.test.extension.junit5.TestComponent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KoraAppTest(Application.class)
class KafkaOutboxSenderIT implements KoraAppTestConfigModifier {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("filter_db")
            .withUsername("filter")
            .withPassword("filter")
            .withInitScript("init-filter_outbox.sql");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.1")
    );

    @TestComponent
    KafkaService kafkaService;

    @NotNull
    @Override
    public KoraConfigModification config() {
        return KoraConfigModification.ofString("""
                httpServer {
                  publicApiHttpPort = 0
                  privateApiHttpPort = 0
                }
                db {
                  jdbcUrl = ${POSTGRES_JDBC_URL}
                  username = ${POSTGRES_USER}
                  password = ${POSTGRES_PASS}
                  schema = "public"
                  poolName = "kora"
                  maxPoolSize = 5
                  minIdle = 0
                  connectionTimeout = "10s"
                  validationTimeout = "5s"
                  idleTimeout = "10m"
                  maxLifetime = "15m"
                  leakDetectionThreshold = "0s"
                  initializationFailTimeout = "0s"
                  readinessProbe = false
                }
                scheduling {
                  threads = 1
                  shutdownWait = "5s"
                }
                kafka {
                  EventProducer {
                    topic = "filter-topic"
                    driverProperties {
                      "bootstrap.servers" = ${KAFKA_BOOTSTRAP}
                      "acks" = "all"
                      "enable.idempotence" = true
                      "delivery.timeout.ms" = 60000
                      "request.timeout.ms" = 30000
                      "retry.backoff.ms" = 100
                    }
                  }
                }
                """)
                .withSystemProperty("POSTGRES_JDBC_URL", POSTGRES.getJdbcUrl())
                .withSystemProperty("POSTGRES_USER", POSTGRES.getUsername())
                .withSystemProperty("POSTGRES_PASS", POSTGRES.getPassword())
                .withSystemProperty("KAFKA_BOOTSTRAP", KAFKA.getBootstrapServers());
    }

    @BeforeEach
    void cleanOutbox() throws Exception {
        try (Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM filter_outbox");
        }
    }

    @Test
    void send_publishesToKafkaWithKeyAndHeader_thenDeletesRow() throws Exception {
        final String topic = "filter-topic";
        final long filterId = 42L;
        final int userId = 7;
        final String expectedKey = "IMPULSE:" + filterId;

        long eventId;
        try (Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var ps = c.prepareStatement("""
                     INSERT INTO filter_outbox (action, operation, filter_id, user_id, payload, created_at)
                     VALUES ('IMPULSE', 2, ?, ?, NULL, NOW())
                     RETURNING id
                     """)) {
            ps.setLong(1, filterId);
            ps.setInt(2, userId);
            try (var rs = ps.executeQuery()) {
                rs.next();
                eventId = rs.getLong(1);
            }
        }

        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.subscribe(List.of(topic));
            consumer.poll(Duration.ofMillis(500));

            kafkaService.send();

            List<ConsumerRecord<String, String>> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(1000)).forEach(received::add);
                assertThat(received).isNotEmpty();
            });

            ConsumerRecord<String, String> rec = received.stream()
                    .filter(r -> r.topic().equals(topic))
                    .findFirst()
                    .orElseThrow();

            assertThat(rec.key()).isEqualTo(expectedKey);
            String eventIdHeader = new String(rec.headers().lastHeader("event-id").value());
            assertThat(eventIdHeader).isEqualTo(Long.toString(eventId));
            assertThat(rec.value()).contains("\"action\":\"IMPULSE\"");
            assertThat(rec.value()).contains("\"operation\":\"SUBSCRIBE\"");
        }

        try (Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var ps = c.prepareStatement("SELECT COUNT(*) FROM filter_outbox WHERE id = ?")) {
            ps.setLong(1, eventId);
            try (var rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            }
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "sender-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return new KafkaConsumer<>(props);
    }
}