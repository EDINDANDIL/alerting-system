package ru.services;

import ru.persistence.entity.UserEntity;
import ru.persistence.repository.UserRepository;
import ru.tinkoff.kora.common.Component;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class UserService {

    private final UserRepository userRepository;
    private final DBExecutor executor;

    public UserService(UserRepository userRepository, DBExecutor executor) {
        this.userRepository = userRepository;
        this.executor = executor;
    }

    public CompletionStage<UserEntity> register(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
            return userRepository.insert(email, passwordHash);
        }, executor.executor());
    }

    public CompletionStage<Optional<UserEntity>> findByEmail(String email) {
        return CompletableFuture.supplyAsync(() -> userRepository.findByEmail(email), executor.executor());
    }

    public CompletionStage<Boolean> checkPassword(String plainPassword, String hashedPassword) {
        return CompletableFuture.supplyAsync(() -> BCrypt.checkpw(plainPassword, hashedPassword), executor.executor());
    }
}