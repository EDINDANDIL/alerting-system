package ru.persistence.repository;
import ru.common.persistence.entity.FilterOutboxEntity;
import ru.tinkoff.kora.database.common.annotation.Id;
import ru.tinkoff.kora.database.common.annotation.Query;
import ru.tinkoff.kora.database.common.annotation.Repository;
import ru.tinkoff.kora.database.jdbc.JdbcRepository;

import java.util.List;
import java.util.UUID;
@Repository
public interface OutboxRepository extends JdbcRepository {


    @Id
    @Query("""
           INSERT INTO filter_outbox (action, operation, filter_id, user_id, payload, created_at)
           VALUES (:entity.action, :entity.operation, :entity.filterId, :entity.userId, :entity.payload, :entity.createdAt)
           """)
    long insert(FilterOutboxEntity entity);

    @Query("""
            SELECT id, action, operation, filter_id, user_id, payload, created_at
            FROM filter_outbox
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """)
    List<FilterOutboxEntity> findNextBatch(int limit);

    @Query("DELETE FROM filter_outbox WHERE id = :id")
    void deleteById(long id);

}