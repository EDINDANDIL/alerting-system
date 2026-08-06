package ru.services;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.common.dto.FilterCreatedEvent;
import ru.common.mappers.outbox.EventOutboxMapper;
import ru.common.mappers.outbox.OutboxMapperFacade;
import ru.common.persistence.entity.FilterOutboxEntity;
import ru.common.util.OutboxOperation;
import ru.kafka.publishers.Publisher;
import ru.persistence.repository.OutboxRepository;
import ru.tinkoff.kora.database.common.QueryContext;
import ru.tinkoff.kora.database.common.telemetry.DataBaseTelemetry;
import ru.tinkoff.kora.database.jdbc.ConnectionContext;
import ru.tinkoff.kora.database.jdbc.JdbcConnectionFactory;
import ru.tinkoff.kora.database.jdbc.JdbcHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventSchedulerTest {

    private OutboxRepository outboxRepository;
    private Publisher publisher;
    private OutboxMapperFacade mapperFacade;
    private EventScheduler scheduler;

    @BeforeEach
    void setUp() {
        outboxRepository = mock(OutboxRepository.class);
        publisher = mock(Publisher.class);
        mapperFacade = new OutboxMapperFacade(new TestEventOutboxMapper());

        JdbcConnectionFactory tx = new ImmediateTxConnectionFactory();
        when(outboxRepository.getJdbcConnectionFactory()).thenReturn(tx);

        scheduler = new EventScheduler(outboxRepository, publisher, mapperFacade);
    }

    @Test
    void send_processesBatchSuccessfully() {
        FilterOutboxEntity entity1 = new FilterOutboxEntity(
                1L, "IMPULSE", OutboxOperation.CREATE, 10L, 1, null, OffsetDateTime.now()
        );
        FilterOutboxEntity entity2 = new FilterOutboxEntity(
                2L, "IMPULSE", OutboxOperation.SUBSCRIBE, 10L, 1, null, OffsetDateTime.now()
        );

        when(outboxRepository.findNextBatchToProcess(10)).thenReturn(List.of(entity1, entity2));
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("filter-topic", 0), 0, 0, 0, 0, 0);
        when(publisher.send(any())).thenReturn(metadata);

        scheduler.send();

        verify(outboxRepository).findNextBatchToProcess(10);
        verify(publisher, times(2)).send(any());
        verify(outboxRepository).deleteById(1L);
        verify(outboxRepository).deleteById(2L);
        verify(outboxRepository, never()).updateStatus(anyLong(), anyString(), anyString());
    }

    @Test
    void send_handlesKafkaSendFailurePerItem() {
        FilterOutboxEntity entity1 = new FilterOutboxEntity(
                1L, "IMPULSE", OutboxOperation.CREATE, 10L, 1, null, OffsetDateTime.now()
        );
        FilterOutboxEntity entity2 = new FilterOutboxEntity(
                2L, "IMPULSE", OutboxOperation.SUBSCRIBE, 10L, 1, null, OffsetDateTime.now()
        );

        when(outboxRepository.findNextBatchToProcess(10)).thenReturn(List.of(entity1, entity2));

        // Entity 1 fails during send
        doThrow(new RuntimeException("Kafka producer error"))
                .doAnswer(invocation -> new RecordMetadata(new TopicPartition("filter-topic", 0), 0, 0, 0, 0, 0))
                .when(publisher).send(any());

        scheduler.send();

        verify(outboxRepository).findNextBatchToProcess(10);
        verify(publisher, times(2)).send(any());

        // Entity 1 should be marked FAILED and NOT deleted
        verify(outboxRepository, never()).deleteById(1L);
        verify(outboxRepository).updateStatus(eq(1L), eq("FAILED"), contains("Kafka producer error"));

        // Entity 2 should succeed and be deleted
        verify(outboxRepository).deleteById(2L);
    }

    @Test
    void send_handlesKafkaException() {
        FilterOutboxEntity entity = new FilterOutboxEntity(
                1L, "IMPULSE", OutboxOperation.CREATE, 10L, 1, null, OffsetDateTime.now()
        );

        when(outboxRepository.findNextBatchToProcess(10)).thenReturn(List.of(entity));
        doThrow(new org.apache.kafka.common.KafkaException("Kafka cluster unreachable"))
                .when(publisher).send(any());

        scheduler.send();

        verify(outboxRepository, never()).deleteById(1L);
        verify(outboxRepository).updateStatus(eq(1L), eq("FAILED"), contains("Kafka cluster unreachable"));
    }

    @Test
    void send_handlesOutboxMapperException() {
        FilterOutboxEntity entity = new FilterOutboxEntity(
                1L, "UNKNOWN_ACTION", OutboxOperation.CREATE, 10L, 1, null, OffsetDateTime.now()
        );

        when(outboxRepository.findNextBatchToProcess(10)).thenReturn(List.of(entity));

        scheduler.send();

        verify(publisher, never()).send(any());
        verify(outboxRepository, never()).deleteById(1L);
        verify(outboxRepository).updateStatus(eq(1L), eq("FAILED"), anyString());
    }

    @Test
    void send_handlesEmptyBatch() {
        when(outboxRepository.findNextBatchToProcess(10)).thenReturn(List.of());

        scheduler.send();

        verify(outboxRepository).findNextBatchToProcess(10);
        verifyNoInteractions(publisher);
        verify(outboxRepository, never()).deleteById(anyLong());
        verify(outboxRepository, never()).updateStatus(anyLong(), anyString(), anyString());
    }

    private static final class TestEventOutboxMapper implements EventOutboxMapper {
        @Override
        public FilterOutboxEntity asEntity(FilterCreatedEvent event) {
            return new FilterOutboxEntity(
                    null,
                    event.action(),
                    event.operation(),
                    event.filterId(),
                    Math.toIntExact(event.userId()),
                    event.payload(),
                    event.createdAt()
            );
        }

        @Override
        public FilterCreatedEvent asEvent(FilterOutboxEntity entity) {
            if ("UNKNOWN_ACTION".equals(entity.action())) {
                throw new IllegalArgumentException("Unknown action: " + entity.action());
            }
            return new FilterCreatedEvent(
                    entity.action(),
                    entity.operation(),
                    entity.filterId(),
                    entity.userId(),
                    entity.createdAt(),
                    entity.payload()
            );
        }
    }

    private static final class ImmediateTxConnectionFactory implements JdbcConnectionFactory {
        private final ConnectionContext context = mock(ConnectionContext.class);

        @Override
        public <T> T inTx(JdbcHelper.SqlFunction1<Connection, T> callback) {
            try {
                return callback.apply(null);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ConnectionContext currentConnectionContext() {
            return context;
        }

        @Override
        public <T> T withConnection(JdbcHelper.SqlFunction1<Connection, T> callback) {
            try {
                return callback.apply(null);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Connection currentConnection() {
            return null;
        }

        @Override
        public Connection newConnection() {
            return null;
        }

        @Override
        public DataBaseTelemetry telemetry() {
            return null;
        }

        @Override
        public <T> T query(QueryContext queryContext, JdbcHelper.SqlFunction1<PreparedStatement, T> callback) {
            try {
                return callback.apply(null);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
