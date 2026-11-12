package br.gov.quixada.sports.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.gov.quixada.sports.security.auth.exception.EmailAlreadyRegisteredException;
import br.gov.quixada.sports.security.users.User;
import br.gov.quixada.sports.security.users.UserRole;
import br.gov.quixada.sports.security.users.dto.RegisterRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder);
    }

    private RegisterRequest aRequest() {
        return new RegisterRequest("john", "john@example.com", "password123");
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register user with encrypted password and role USER")
        void shouldRegisterUser() {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hash");

            service.register(aRequest());

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User saved = captor.getValue();
            assertThat(saved.getUsername()).isEqualTo("john");
            assertThat(saved.getEmail()).isEqualTo("john@example.com");
            assertThat(saved.getPassword()).isEqualTo("hash");
            assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("should allow duplicate username")
        void shouldAllowDuplicateUsername() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");

            service.register(aRequest());
            service.register(aRequest());

            verify(userRepository, times(2)).save(any(User.class));
        }

        @Test
        @DisplayName("should throw EmailAlreadyRegisteredException when the email already exists")
        void shouldThrowWhenEmailAlreadyRegistered() {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> service.register(aRequest()))
                    .isInstanceOf(EmailAlreadyRegisteredException.class)
                    .hasMessage("Email already registered");

            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("should convert the email unique index violation into EmailAlreadyRegisteredException")
        void shouldConvertEmailViolation() {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hash");
            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException(
                            "Duplicate entry 'john@example.com' for key 'db_users.uk_db_users_email'"));

            assertThatThrownBy(() -> service.register(aRequest()))
                    .isInstanceOf(EmailAlreadyRegisteredException.class)
                    .hasMessage("Email already registered");
        }

        @Test
        @DisplayName("should rethrow an unknown integrity violation")
        void shouldRethrowUnknownViolation() {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hash");
            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("another violation"));

            assertThatThrownBy(() -> service.register(aRequest()))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessage("another violation");
        }
    }
}
