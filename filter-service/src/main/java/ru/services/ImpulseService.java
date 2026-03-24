package ru.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.common.dto.OutboxCreatedEvent;
import ru.models.dto.Request;
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

@Component
public final class ImpulseService implements FilterService {

    private final ImpulseFiltersRepository impFiltersRepo;
    private final UserImpulseFiltersRepository userImpFilterRepo;
    private final OutboxRepository outboxRepository;
    private final ImpulseFilterMapper impulseMapper;
    private final OutboxMapperFacade mapperFacade;
    private static final Logger log = LoggerFactory.getLogger(ImpulseService.class);

    public ImpulseService(ImpulseFiltersRepository impFiltersRepo, UserImpulseFiltersRepository userImpFilterRepo, OutboxRepository outboxRepository, ImpulseFilterMapper impulseMapper, OutboxMapperFacade mapperFacade) {
        this.impFiltersRepo = impFiltersRepo;
        this.userImpFilterRepo = userImpFilterRepo;
        this.outboxRepository = outboxRepository;
        this.impulseMapper = impulseMapper;
        this.mapperFacade = mapperFacade;
    }

    @Override
    public void subscribe(int userId, Request dto) {

        Request.ImpulseFilterDto impulseFilterDto = (Request.ImpulseFilterDto) dto;

        impFiltersRepo.getJdbcConnectionFactory().inTx(connection -> {

            var connectionContext = impFiltersRepo.getJdbcConnectionFactory().currentConnectionContext();

            assert connectionContext != null;
            connectionContext.addPostCommitAction(conn ->
                    log.info("Transaction COMMITTED: user with id {} subscribed to filter {}", userId, impulseFilterDto)
            );
            connectionContext.addPostRollbackAction((conn, e) ->
                    log.error("Transaction ROLLBACK for user {} due to: {}", userId, e.getMessage(), e)
            );

            ImpulseFilterEntity impulseFilterEntity = impFiltersRepo.findByConfig(impulseMapper.toEntity(impulseFilterDto))
                    .map(f -> {
                        log.info("Filter already exists with id {}, reusing", f.id());
                        return f;
                    })
                    .orElseGet(() -> {

                        ImpulseFilterEntity newImpulseFilterEntity = impulseMapper.toEntity(impulseFilterDto);
                        long newFilterId = impFiltersRepo.insert(newImpulseFilterEntity);

                        OutboxCreatedEvent event = new OutboxCreatedEvent(
                        impulseFilterDto.action(),
                        OutboxOperation.CREATE,
                        newFilterId,
                        userId,
                        OffsetDateTime.now(),
                        impulseMapper.toOutboxPayload(impulseFilterDto)
                        );

                        long eventId = outboxRepository.insert(mapperFacade.asEntity(event));

                        log.info("Created new filter with id {}", newFilterId);
                        log.info("Created new event CREATE with id {}", eventId);

                return new ImpulseFilterEntity(
                        newFilterId,
                        newImpulseFilterEntity.exchange(),
                        newImpulseFilterEntity.market(),
                        newImpulseFilterEntity.blackList(),
                        newImpulseFilterEntity.action(),
                        newImpulseFilterEntity.timeWindow(),
                        newImpulseFilterEntity.direction(),
                        newImpulseFilterEntity.percent(),
                        newImpulseFilterEntity.volume24h()
                );
            });

            long newFilterId = impulseFilterEntity.id();

            UpdateCount count = userImpFilterRepo.subscribe(userId, newFilterId);

            OutboxCreatedEvent event = new OutboxCreatedEvent(
                    impulseFilterDto.action(),
                    OutboxOperation.SUBSCRIBE,
                    newFilterId,
                    userId,
                    OffsetDateTime.now(),
                    null
            );

            long eventCreatedId = outboxRepository.insert(mapperFacade.asEntity(event));
            log.info("Created new event SUBSCRIBE with id {}", eventCreatedId);

            return impulseFilterEntity;
        });
    }

    @Override
    public void unsubscribe(int userId, Request dto) throws FilterNotFoundException, UserNotFoundException {

        Request.ImpulseFilterDto impulseFilterDto = (Request.ImpulseFilterDto) dto;

        userImpFilterRepo.getJdbcConnectionFactory().inTx(connection -> {
            var connectionContext = userImpFilterRepo.getJdbcConnectionFactory().currentConnectionContext();

            assert connectionContext != null;
            connectionContext.addPostCommitAction(conn ->
                    log.info("Transaction COMMITTED: user {} unsubscribed from filter {}", userId, impulseFilterDto)
            );
            connectionContext.addPostRollbackAction((conn, e) ->
                    log.error("Transaction ROLLBACK for user {} due to: {}", userId, e.getMessage(), e)
            );

            ImpulseFilterEntity impulseFilterEntity = impFiltersRepo.findByConfig(impulseMapper.toEntity(impulseFilterDto))
                    .orElseThrow(() -> new FilterNotFoundException("Filter with current configuration not found"));

            UpdateCount updateCount = userImpFilterRepo.unsubscribe(userId, impulseFilterEntity.id());

            if (updateCount.value() == 0) {
                log.warn("User {} was not subscribed to filter {}", userId, impulseFilterEntity.id());
                throw new UserNotFoundException("User with current id not found or not subscribed to this filter");
            }

            long filterId = impulseFilterEntity.id();

            long subscribersCount = userImpFilterRepo.countByImpulseId(filterId);

            OutboxCreatedEvent unsubEvent = new OutboxCreatedEvent(
                    impulseFilterDto.action(),
                    OutboxOperation.UNSUBSCRIBE,
                    filterId,
                    userId,
                    OffsetDateTime.now(),
                    null
            );

            long unsubscribedEventId = outboxRepository.insert(mapperFacade.asEntity(unsubEvent));
            log.info("Created new event DELETE with id {}", unsubscribedEventId);

            if (subscribersCount == 0) {
                long deletedId = impulseFilterEntity.id();
                UpdateCount deleteResult = impFiltersRepo.deleteById(deletedId);

                OutboxCreatedEvent deleteEvent = new OutboxCreatedEvent(
                        impulseFilterDto.action(),
                        OutboxOperation.DELETE,
                        filterId,
                        userId,
                        OffsetDateTime.now(),
                        impulseMapper.toOutboxPayload(impulseFilterDto)
                );

                long deletedEventId = outboxRepository.insert(mapperFacade.asEntity(deleteEvent));

                log.info("Filter {} deleted as it has no more subscribers", impulseFilterEntity.id());
                log.info("Created new event DELETE with id {}", deletedEventId);
            }
            return impulseFilterEntity;
        });
    }
}