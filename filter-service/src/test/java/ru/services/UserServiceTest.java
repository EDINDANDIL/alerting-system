package ru.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import ru.persistence.entity.UserEntity;
import ru.persistence.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void register_hashesPasswordAndInsertsUser() {
        String email = "test@example.com";
        String password = "secretPassword123";
        UserEntity expectedUser = new UserEntity(1L, email, "hashedPassword");

        when(userRepository.insert(eq(email), anyString())).thenReturn(expectedUser);

        UserEntity result = userService.register(email, password);

        assertEquals(expectedUser, result);
        verify(userRepository).insert(eq(email), argThat(hash -> BCrypt.checkpw(password, hash)));
    }

    @Test
    void findByEmail_returnsUserWhenFound() {
        String email = "user@domain.com";
        UserEntity user = new UserEntity(2L, email, "hash");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<UserEntity> result = userService.findByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void findByEmail_returnsEmptyWhenNotFound() {
        String email = "missing@domain.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Optional<UserEntity> result = userService.findByEmail(email);

        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void checkPassword_validatesPasswordCorrectly() {
        String rawPassword = "mySecurePassword";
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

        assertTrue(userService.checkPassword(rawPassword, hash));
        assertFalse(userService.checkPassword("wrongPassword", hash));
    }
}
