package ru.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.common.dto.OutboxCreatedEvent;
import ru.models.dto.Request;
import ru.models.dto.Response;
import ru.models.exceptions.FilterNotFoundException;
import ru.models.exceptions.UserNotFoundException;
import ru.models.mappers.impulse.ImpulseFilterMapper;
import ru.common.mappers.outbox.OutboxMapperFacade;
import ru.persistence.entity.ImpulseFilterEntity;
import ru.persistence.repository.ImpulseFiltersRepository;
import ru.persistence.repository.OutboxRepository;
import ru.persistence.repository.UserImpulseFiltersRepository;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.database.common.UpdateCount;
import ru.common.util.OutboxOperation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public final class ImpulseService implements FilterService {

    private final ImpulseFiltersRepository impFiltersRepo;
    private final UserImpulseFiltersRepository userImpFilterRepo;
    private final OutboxRepository outboxRepository;
    private final ImpulseFilterMapper impulseMapper;
    private final OutboxMapperFacade mapperFacade;
    private final DBExecutor executor;
    private static final Logger log = LoggerFactory.getLogger(ImpulseService.class);

    public ImpulseService(ImpulseFiltersRepository impFiltersRepo, UserImpulseFiltersRepository userImpFilterRepo, OutboxRepository outboxRepository, ImpulseFilterMapper impulseMapper, OutboxMapperFacade mapperFacade, DBExecutor executor) {
        this.impFiltersRepo = impFiltersRepo;
        this.userImpFilterRepo = userImpFilterRepo;
        this.outboxRepository = outboxRepository;
        this.impulseMapper = impulseMapper;
        this.mapperFacade = mapperFacade;
        this.executor = executor;
    }

    @Override
    public CompletionStage<Response> subscribe(long userId, Request dto) {
        return CompletableFuture.supplyAsync(
                () -> performSubscribe(userId, (Request.ImpulseFilterDto) dto),
                executor.executor()
        );
    }

    @Override
    public CompletionStage<Void> unsubscribe(long userId, long filterId) {
        return CompletableFuture.runAsync(
                () -> performUnsubscribe(userId, filterId),
                executor.executor()
        );
    }

    @Override
    public CompletableFuture<List<Response>> findFiltersByUserId(long id) {
        return CompletableFuture.supplyAsync(
                () -> impFiltersRepo.findFiltersByUserId(id).stream()
                            .map(impulseMapper::toResponse)
                            .map(Response.class::cast)
                            .toList(),
                executor.executor()
        );
    }

    private Response performSubscribe(long userId, Request.ImpulseFilterDto dto) {

        ImpulseFilterEntity entity = impFiltersRepo.getJdbcConnectionFactory().inTx(_ -> {

            var connectionContext = impFiltersRepo.getJdbcConnectionFactory().currentConnectionContext();
            assert connectionContext != null;
            connectionContext.addPostCommitAction(_ ->
                    log.info("Transaction COMMITTED: user with id {} subscribed to filter {}", userId, dto)
            );
            connectionContext.addPostRollbackAction((_, e) ->
                    log.error("Transaction ROLLBACK for user {} due to: {}", userId, e.getMessage(), e)
            );
            ImpulseFilterEntity impulseFilterEntity = impFiltersRepo.findByConfig(impulseMapper.toEntity(dto))
                    .map(f -> {
                        log.info("Filter already exists with id {}, reusing", f.id());
                        return f;
                    })
                    .orElseGet(() -> {

                        ImpulseFilterEntity newImpulseFilterEntity = impFiltersRepo.insert(impulseMapper.toEntity(dto));

                        OutboxCreatedEvent event = new OutboxCreatedEvent(
                                dto.action(),
                                OutboxOperation.CREATE,
                                newImpulseFilterEntity.id(),
                                userId,
                                OffsetDateTime.now(),
                                impulseMapper.toOutboxPayload(dto)
                        );

                        long eventId = outboxRepository.insert(mapperFacade.asEntity(event));

                        log.info("Created new filter with id {}", newImpulseFilterEntity.id());
                        log.info("Created new event CREATE with id {}", eventId);

                        return newImpulseFilterEntity;

                    });

            long newFilterId = impulseFilterEntity.id();

            UpdateCount count = userImpFilterRepo.subscribe(userId, newFilterId);

            if (count.value() > 0) {
                OutboxCreatedEvent event = new OutboxCreatedEvent(
                        dto.action(),
                        OutboxOperation.SUBSCRIBE,
                        newFilterId,
                        userId,
                        OffsetDateTime.now(),
                        null
                );

                long eventCreatedId = outboxRepository.insert(mapperFacade.asEntity(event));
                log.info("Created new event SUBSCRIBE with id {}", eventCreatedId);
            }
            return impulseFilterEntity;
        });
        return impulseMapper.toResponse(entity);
    }

    private void performUnsubscribe(long  userId, long filterId) throws FilterNotFoundException, UserNotFoundException {

        userImpFilterRepo.getJdbcConnectionFactory().inTx(_ -> {
            var connectionContext = userImpFilterRepo.getJdbcConnectionFactory().currentConnectionContext();

            assert connectionContext != null;
            connectionContext.addPostCommitAction(_ ->
                    log.info("Transaction COMMITTED: user {} unsubscribed from filter {}", userId, filterId)
            );
            connectionContext.addPostRollbackAction((_, e) ->
                    log.error("Transaction ROLLBACK for user {} due to: {}", userId, e.getMessage(), e)
            );

            ImpulseFilterEntity impulseFilterEntity = impFiltersRepo.findFilterById(filterId)
                    .orElseThrow(() -> new FilterNotFoundException("Filter with current configuration not found"));

            UpdateCount updateCount = userImpFilterRepo.unsubscribe(userId, impulseFilterEntity.id());

            if (updateCount.value() == 0) {
                log.warn("User {} was not subscribed to filter {}", userId, impulseFilterEntity.id());
                throw new UserNotFoundException("User with current id not found or not subscribed to this filter");
            }

            OutboxCreatedEvent unsubEvent = new OutboxCreatedEvent(
                    impulseFilterEntity.action(),
                    OutboxOperation.UNSUBSCRIBE,
                    filterId,
                    userId,
                    OffsetDateTime.now(),
                    null
            );

            long unsubscribedEventId = outboxRepository.insert(mapperFacade.asEntity(unsubEvent));
            log.info("Created new event UNSUBSCRIBE with id {}", unsubscribedEventId);

            long subscribersCount = userImpFilterRepo.countByImpulseId(filterId);

            if (subscribersCount == 0) {
                long deletedId = impulseFilterEntity.id();
                UpdateCount deleteResult = impFiltersRepo.deleteById(deletedId);

                if (deleteResult.value() == 0) throw new FilterNotFoundException("Filter not found during delete");

                OutboxCreatedEvent deleteEvent = new OutboxCreatedEvent(
                        impulseFilterEntity.action(),
                        OutboxOperation.DELETE,
                        filterId,
                        userId,
                        OffsetDateTime.now(),
                        null
                );

                long deletedEventId = outboxRepository.insert(mapperFacade.asEntity(deleteEvent));

                log.info("Filter {} deleted as it has no more subscribers", impulseFilterEntity.id());
                log.info("Created new event DELETE with id {}", deletedEventId);
            }
            return impulseFilterEntity;
        });
    }
}