package ru.persistence.repository;

import java.util.List;
import java.util.Optional;

import ru.persistence.entity.ImpulseFilterEntity;
import ru.tinkoff.kora.database.common.UpdateCount;
import ru.tinkoff.kora.database.common.annotation.Id;
import ru.tinkoff.kora.database.common.annotation.Query;
import ru.tinkoff.kora.database.common.annotation.Repository;
import ru.tinkoff.kora.database.jdbc.JdbcRepository;

@Repository
public interface ImpulseFiltersRepository extends JdbcRepository {

    @Query("""
           SELECT id,
                  exchanges,
                  markets,
                  blacklist,
                  action,
                  time_window_sec,
                  direction_code,
                  percent,
                  volume_24h
           FROM impulse_filters
           WHERE id = :id
           """)
    Optional<ImpulseFilterEntity> findById(long id);

    @Query("""
           SELECT id,
                  exchanges,
                  markets,
                  blacklist,
                  action,
                  time_window_sec,
                  direction_code,
                  percent,
                  volume_24h
           FROM impulse_filters
           WHERE exchanges       = :entity.exchange
            AND markets         = :entity.market
            AND blacklist       = :entity.blackList
            AND action          = :entity.action
            AND time_window_sec = :entity.timeWindow
            AND direction_code  = :entity.direction
            AND percent         = :entity.percent
            AND volume_24h      = :entity.volume24h
           """)
    Optional<ImpulseFilterEntity> findByConfig(ImpulseFilterEntity entity);

    @Id
    @Query("""
           INSERT INTO impulse_filters (
               exchanges,
               markets,
               blacklist,
               action,
               time_window_sec,
               direction_code,
               percent,
               volume_24h
           )
           VALUES (
               :entity.exchange,
               :entity.market,
               :entity.blackList,
               :entity.action,
               :entity.timeWindow,
               :entity.direction,
               :entity.percent,
               :entity.volume24h
           )
           """)
    long insert(ImpulseFilterEntity entity);

    @Query("""
           DELETE FROM impulse_filters
           WHERE id = :id
           """)
    UpdateCount deleteById(long id);

    @Query("""
           SELECT id,
                  exchanges,
                  markets,
                  blacklist,
                  action,
                  time_window_sec,
                  direction_code,
                  percent,
                  volume_24h
           FROM impulse_filters
           """)
    List<ImpulseFilterEntity> findAll();
}