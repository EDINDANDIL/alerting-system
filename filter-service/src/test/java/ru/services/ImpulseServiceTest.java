package ru.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.common.dto.FilterCreatedEvent;
import ru.common.dto.OutboxPayload;
import ru.common.mappers.outbox.EventOutboxMapper;
import ru.common.mappers.outbox.OutboxMapperFacade;
import ru.common.persistence.entity.FilterOutboxEntity;
import ru.common.util.Direction;
import ru.common.util.OutboxOperation;
import ru.models.dto.Request;
import ru.models.dto.Response;
import ru.models.exceptions.FilterNotFoundException;
import ru.models.exceptions.UserNotFoundException;
import ru.models.mappers.impulse.ImpulseFilterMapper;
import ru.persistence.entity.ImpulseFilterEntity;
import ru.persistence.repository.ImpulseFiltersRepository;
import ru.persistence.repository.OutboxRepository;
import ru.persistence.repository.UserImpulseFiltersRepository;
import ru.tinkoff.kora.database.common.QueryContext;
import ru.tinkoff.kora.database.common.UpdateCount;
import ru.tinkoff.kora.database.common.telemetry.DataBaseTelemetry;
import ru.tinkoff.kora.database.jdbc.ConnectionContext;
import ru.tinkoff.kora.database.jdbc.JdbcConnectionFactory;
import ru.tinkoff.kora.database.jdbc.JdbcHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImpulseServiceTest {

    private static final long USER_ID = 42L;
    private static final long FILTER_ID = 10L;

    private ImpulseFiltersRepository filtersRepository;
    private UserImpulseFiltersRepository userFiltersRepository;
    private OutboxRepository outboxRepository;
    private DBExecutor dbExecutor;
    private ImpulseService service;

    @BeforeEach
    void setUp() {
        filtersRepository = mock(ImpulseFiltersRepository.class);
        userFiltersRepository = mock(UserImpulseFiltersRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        dbExecutor = new DBExecutor();

        JdbcConnectionFactory tx = new ImmediateTxConnectionFactory();
        when(filtersRepository.getJdbcConnectionFactory()).thenReturn(tx);
        when(userFiltersRepository.getJdbcConnectionFactory()).thenReturn(tx);

        service = new ImpulseService(
                filtersRepository,
                userFiltersRepository,
                outboxRepository,
                new TestImpulseFilterMapper(),
                new OutboxMapperFacade(new TestEventOutboxMapper()),
                dbExecutor
        );
    }

    @AfterEach
    void tearDown() {
        dbExecutor.close();
    }

    @Test
    void subscribe_createsFilterSubscribesUserAndWritesCreateAndSubscribeEvents() {
        Request.ImpulseFilterDto request = request();
        ImpulseFilterEntity created = entity(FILTER_ID);

        when(filtersRepository.findByConfig(any())).thenReturn(Optional.empty());
        when(filtersRepository.insert(any())).thenReturn(created);
        when(userFiltersRepository.subscribe(USER_ID, FILTER_ID)).thenReturn(new UpdateCount(1));
        when(outboxRepository.insert(any())).thenReturn(100L, 101L);

        Response response = service.subscribe(USER_ID, request).toCompletableFuture().join();

        assertEquals(new TestImpulseFilterMapper().toResponse(created), response);
        verify(filtersRepository).insert(any());
        verify(userFiltersRepository).subscribe(USER_ID, FILTER_ID);

        List<FilterOutboxEntity> events = capturedOutboxEvents();
        assertEquals(List.of(OutboxOperation.CREATE, OutboxOperation.SUBSCRIBE),
                events.stream().map(FilterOutboxEntity::operation).toList());
        assertNotNull(events.getFirst().payload());
        assertNull(events.get(1).payload());
    }

    @Test
    void subscribe_reusesExistingFilterAndDoesNotWriteSubscribeEventWhenAlreadySubscribed() {
        Request.ImpulseFilterDto request = request();
        ImpulseFilterEntity existing = entity(FILTER_ID);

        when(filtersRepository.findByConfig(any())).thenReturn(Optional.of(existing));
        when(userFiltersRepository.subscribe(USER_ID, FILTER_ID)).thenReturn(new UpdateCount(0));

        Response response = service.subscribe(USER_ID, request).toCompletableFuture().join();

        assertEquals(new TestImpulseFilterMapper().toResponse(existing), response);
        verify(filtersRepository, never()).insert(any());
        verify(outboxRepository, never()).insert(any());
    }

    //TODO посмотреть тесты
    @Test
    void subscribe_reusesExistingFilterAndWritesOnlySubscribeEventForNewSubscription() {
        Request.ImpulseFilterDto request = request();
        ImpulseFilterEntity existing = entity(FILTER_ID);

        when(filtersRepository.findByConfig(any())).thenReturn(Optional.of(existing));
        when(userFiltersRepository.subscribe(USER_ID, FILTER_ID)).thenReturn(new UpdateCount(1));
        when(outboxRepository.insert(any())).thenReturn(100L);

        Response response = service.subscribe(USER_ID, request).toCompletableFuture().join();

        assertEquals(new TestImpulseFilterMapper().toResponse(existing), response);
        verify(filtersRepository, never()).insert(any());
        verify(userFiltersRepository).subscribe(USER_ID, FILTER_ID);

        List<FilterOutboxEntity> events = capturedOutboxEvents();
        assertEquals(1, events.size());
        assertEquals(OutboxOperation.SUBSCRIBE, events.getFirst().operation());
        assertEquals("IMPULSE", events.getFirst().action());
        assertEquals(FILTER_ID, events.getFirst().filterId());
        assertEquals(Math.toIntExact(USER_ID), events.getFirst().userId());
        assertNull(events.getFirst().payload());
    }

    //TODO посмотреть тесты
    @Test
    void subscribe_createAndSubscribeEventsContainExpectedFields() {
        Request.ImpulseFilterDto request = request();
        ImpulseFilterEntity created = entity(FILTER_ID);

        when(filtersRepository.findByConfig(any())).thenReturn(Optional.empty());
        when(filtersRepository.insert(any())).thenReturn(created);
        when(userFiltersRepository.subscribe(USER_ID, FILTER_ID)).thenReturn(new UpdateCount(1));
        when(outboxRepository.insert(any())).thenReturn(100L, 101L);

        service.subscribe(USER_ID, request).toCompletableFuture().join();

        List<FilterOutboxEntity> events = capturedOutboxEvents();
        FilterOutboxEntity create = events.get(0);
        FilterOutboxEntity subscribe = events.get(1);

        assertEquals("IMPULSE", create.action());
        assertEquals(OutboxOperation.CREATE, create.operation());
        assertEquals(FILTER_ID, create.filterId());
        assertEquals(Math.toIntExact(USER_ID), create.userId());
        assertInstanceOf(OutboxPayload.ImpulseFilter.class, create.payload());

        OutboxPayload.ImpulseFilter payload = (OutboxPayload.ImpulseFilter) create.payload();
        assertEquals(Set.copyOf(request.exchange()), payload.exchange());
        assertEquals(Set.copyOf(request.market()), payload.market());
        assertEquals(Set.copyOf(request.blackList()), payload.blackList());
        assertEquals(request.timeWindow(), payload.timeWindow());
        assertEquals(request.direction(), payload.direction());
        assertEquals(request.percent(), payload.percent());
        assertEquals(request.volume24h(), payload.volume24h());

        assertEquals("IMPULSE", subscribe.action());
        assertEquals(OutboxOperation.SUBSCRIBE, subscribe.operation());
        assertEquals(FILTER_ID, subscribe.filterId());
        assertEquals(Math.toIntExact(USER_ID), subscribe.userId());
        assertNull(subscribe.payload());
    }

    @Test
    void unsubscribe_removesSubscriptionAndWritesOnlyUnsubscribeWhenSubscribersRemain() {
        ImpulseFilterEntity existing = entity(FILTER_ID);

        when(filtersRepository.findFilterById(FILTER_ID)).thenReturn(Optional.of(existing));
        when(userFiltersRepository.unsubscribe(USER_ID, FILTER_ID)).thenReturn(new UpdateCount(1));
        when(userFiltersRepository.countByImpulseId(FILTER_ID)).thenReturn(2L);
        when(outboxRepository.insert(any())).thenReturn(200L);

        service.unsubscribe(USER_ID, FILTER_ID).toCompletableFuture().join();

        verify(filtersRepository, never()).deleteById(anyLong());
        List<FilterOutboxEntity> events = capturedOutboxEvents();
        assertEquals(List.of(OutboxOperation.UNSUBSCRIBE),
                events.stream().map(FilterOutboxEntity::operation).toList());
        assertNull(events.getFirst().payload());
    }

    @Test
    void unsubscribe_deletesFilterAndWritesDeleteWhenLastSubscriberRemoved() {
        ImpulseFilterEntity existing = entity(FILTER_ID);

        when(filtersRepository.findFilterById(FILTER_ID)).thenReturn(Optional.of(existing));
        when(userFiltersRepository.unsubscribe(USER_ID, FILTER_ID)).thenReturn(new UpdateCount(1));
        when(userFiltersRepository.countByImpulseId(FILTER_ID)).thenReturn(0L);
        when(filtersRepository.deleteById(FILTER_ID)).thenReturn(new UpdateCount(1));
        when(outboxRepository.insert(any())).thenReturn(200L, 201L);

        service.unsubscribe(USER_ID, FILTER_ID).toCompletableFuture().join();

        verify(filtersRepository).deleteById(FILTER_ID);
        List<FilterOutboxEntity> events = capturedOutboxEvents();
        assertEquals(List.of(OutboxOperation.UNSUBSCRIBE, OutboxOperation.DELETE),
                events.stream().map(FilterOutboxEntity::operation).toList());
        assertNull(events.get(0).payload());
        assertNull(events.get(1).payload());
    }

    //TODO посмотреть тесты
    @Test
    void findFiltersByUserId_returnsMappedResponses() {
        ImpulseFilterEntity first = entity(10L);
        ImpulseFilterEntity second = new ImpulseFilterEntity(
                20L,
                List.of("BYBIT"),
                List.of("SPOT"),
                List.of("ETHUSDT"),
                "IMPULSE",
                120,
                Direction.DOWN,
                5,
                2_000_000L
        );

        when(filtersRepository.findFiltersByUserId(USER_ID)).thenReturn(List.of(first, second));

        List<Response> responses = service.findFiltersByUserId(USER_ID).toCompletableFuture().join();

        assertEquals(List.of(
                new TestImpulseFilterMapper().toResponse(first),
                new TestImpulseFilterMapper().toResponse(second)
        ), responses);
        verify(filtersRepository).findFiltersByUserId(USER_ID);
        verifyNoInteractions(outboxRepository);
        verify(userFiltersRepository, never()).subscribe(anyLong(), anyLong());
        verify(userFiltersRepository, never()).unsubscribe(anyLong(), anyLong());
    }

    //TODO посмотреть тесты
    @Test
    void findFiltersByUserId_returnsEmptyListWhenRepositoryReturnsNoFilters() {
        when(filtersRepository.findFiltersByUserId(USER_ID)).thenReturn(List.of());

        List<Response> responses = service.findFiltersByUserId(USER_ID).toCompletableFuture().join();

        assertTrue(responses.isEmpty());
        verify(filtersRepository).findFiltersByUserId(USER_ID);
        verifyNoInteractions(outboxRepository);
    }

    @Test
    void unsubscribe_failsWhenFilterDoesNotExist() {
        when(filtersRepository.findFilterById(FILTER_ID)).thenReturn(Optional.empty());

        CompletionException error = assertThrows(
                CompletionException.class,
                () -> service.unsubscribe(USER_ID, FILTER_ID).toCompletableFuture().join()
        );

        assertInstanceOf(FilterNotFoundException.class, error.getCause());
        verify(userFiltersRepository, never()).unsubscribe(anyLong(), anyLong());
        verify(outboxRepository, never()).insert(any());
    }

    @Test
    void unsubscribe_failsWhenUserWasNotSubscribed() {
        when(filtersRepository.findFilterById(FILTER_ID)).thenReturn(Optional.of(entity(FILTER_ID)));
        when(userFiltersRepository.unsubscribe(USER_ID, FILTER_ID)).thenReturn(new UpdateCount(0));

        CompletionException error = assertThrows(
                CompletionException.class,
                () -> service.unsubscribe(USER_ID, FILTER_ID).toCompletableFuture().join()
        );

        assertInstanceOf(UserNotFoundException.class, error.getCause());
        verify(outboxRepository, never()).insert(any());
    }

    @Test
    void unsubscribe_failsWhenLastFilterDeleteDidNotAffectRows() {
        when(filtersRepository.findFilterById(FILTER_ID)).thenReturn(Optional.of(entity(FILTER_ID)));
        when(userFiltersRepository.unsubscribe(USER_ID, FILTER_ID)).thenReturn(new UpdateCount(1));
        when(userFiltersRepository.countByImpulseId(FILTER_ID)).thenReturn(0L);
        when(filtersRepository.deleteById(FILTER_ID)).thenReturn(new UpdateCount(0));
        when(outboxRepository.insert(any())).thenReturn(200L);

        CompletionException error = assertThrows(
                CompletionException.class,
                () -> service.unsubscribe(USER_ID, FILTER_ID).toCompletableFuture().join()
        );

        assertInstanceOf(FilterNotFoundException.class, error.getCause());
    }

    private List<FilterOutboxEntity> capturedOutboxEvents() {
        ArgumentCaptor<FilterOutboxEntity> captor = ArgumentCaptor.forClass(FilterOutboxEntity.class);
        verify(outboxRepository, atLeastOnce()).insert(captor.capture());
        return captor.getAllValues();
    }

    private static Request.ImpulseFilterDto request() {
        return new Request.ImpulseFilterDto(
                List.of("BINANCE"),
                List.of("FUTURES"),
                List.of("BTCUSDT"),
                "IMPULSE",
                60,
                Direction.UP,
                3,
                1_000_000
        );
    }

    private static ImpulseFilterEntity entity(long id) {
        return new ImpulseFilterEntity(
                id,
                List.of("BINANCE"),
                List.of("FUTURES"),
                List.of("BTCUSDT"),
                "IMPULSE",
                60,
                Direction.UP,
                3,
                1_000_000L
        );
    }

    private static final class TestImpulseFilterMapper implements ImpulseFilterMapper {
        @Override
        public ImpulseFilterEntity toEntity(Request.ImpulseFilterDto dto) {
            return new ImpulseFilterEntity(
                    null,
                    dto.exchange(),
                    dto.market(),
                    dto.blackList(),
                    dto.action(),
                    Math.toIntExact(dto.timeWindow()),
                    dto.direction(),
                    dto.percent(),
                    dto.volume24h()
            );
        }

        @Override
        public Request.ImpulseFilterDto toDto(ImpulseFilterEntity entity) {
            return new Request.ImpulseFilterDto(
                    entity.exchange(),
                    entity.market(),
                    entity.blackList(),
                    entity.action(),
                    entity.timeWindow(),
                    entity.direction(),
                    entity.percent(),
                    entity.volume24h()
            );
        }

        @Override
        public OutboxPayload.ImpulseFilter toOutboxPayload(Request.ImpulseFilterDto dto) {
            return new OutboxPayload.ImpulseFilter(
                    Set.copyOf(dto.exchange()),
                    Set.copyOf(dto.market()),
                    Set.copyOf(dto.blackList()),
                    dto.timeWindow(),
                    dto.direction(),
                    dto.percent(),
                    dto.volume24h()
            );
        }

        @Override
        public Response.ImpulseFilterResponse toResponse(ImpulseFilterEntity entity) {
            return new Response.ImpulseFilterResponse(
                    entity.id(),
                    entity.exchange(),
                    entity.market(),
                    entity.blackList(),
                    entity.action(),
                    entity.timeWindow(),
                    entity.direction(),
                    entity.percent(),
                    entity.volume24h()
            );
        }
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
