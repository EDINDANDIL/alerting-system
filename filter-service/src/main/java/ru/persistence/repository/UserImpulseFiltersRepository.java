package ru.persistence.repository;

import java.util.List;

import ru.persistence.entity.UserImpulseFilterEntity;
import ru.tinkoff.kora.database.common.UpdateCount;
import ru.tinkoff.kora.database.common.annotation.Query;
import ru.tinkoff.kora.database.common.annotation.Repository;
import ru.tinkoff.kora.database.jdbc.JdbcRepository;

@Repository
public interface UserImpulseFiltersRepository extends JdbcRepository {

    // привязать user ↔ impulse (idempotent: PK(user_id, impulse_id) не даст дубликатов)
    @Query("""
           INSERT INTO user_impulse_filters (user_id, impulse_id)
           VALUES (:userId, :impulseId)
           ON CONFLICT (user_id, impulse_id) DO NOTHING
           """)
    UpdateCount subscribe(int userId, long impulseId);

    // отвязать user ↔ impulse
    @Query("""
           DELETE FROM user_impulse_filters
           WHERE user_id = :userId AND impulse_id = :impulseId
           """)
    UpdateCount unsubscribe(int userId, long impulseId);

    // сколько пользователей подписано на фильтр
    @Query("""
           SELECT COUNT(*)
           FROM user_impulse_filters
           WHERE impulse_id = :impulseId
           """)
    long countByImpulseId(long impulseId);

    // все фильтры пользователя
    @Query("""
           SELECT user_id, impulse_id, created_at
           FROM user_impulse_filters
           WHERE user_id = :userId
           """)
    List<UserImpulseFilterEntity> findByUserId(int userId);

    // все пользователи конкретного фильтра
    @Query("""
           SELECT user_id, impulse_id, created_at
           FROM user_impulse_filters
           WHERE impulse_id = :impulseId
           """)
    List<UserImpulseFilterEntity> findByImpulseId(long impulseId);
}