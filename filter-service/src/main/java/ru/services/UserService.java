package ru.services;

import ru.persistence.entity.UserEntity;
import ru.persistence.repository.UserRepository;
import ru.tinkoff.kora.common.Component;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Optional;

@Component
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserEntity register(String email, String password) {
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        return userRepository.insert(email, passwordHash);
    }

    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}