package ru.persistence.repository;

import ru.persistence.entity.UserEntity;
import ru.tinkoff.kora.database.common.annotation.Query;
import ru.tinkoff.kora.database.common.annotation.Repository;
import ru.tinkoff.kora.database.jdbc.JdbcRepository;
import java.util.Optional;

@Repository
public interface UserRepository extends JdbcRepository {

    @Query("""
           INSERT INTO users (email, password_hash)
           VALUES (:email, :passwordHash)
           RETURNING id, email, password_hash
           """)
    UserEntity insert(String email, String passwordHash);

    @Query("""
           SELECT id, email, password_hash
           FROM users
           WHERE email = :email
           """)
    Optional<UserEntity> findByEmail(String email);
}