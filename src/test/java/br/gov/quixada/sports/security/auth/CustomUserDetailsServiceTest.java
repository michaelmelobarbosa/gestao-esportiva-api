package br.gov.quixada.sports.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import br.gov.quixada.sports.security.users.User;
import br.gov.quixada.sports.security.users.UserDetailsAdapter;
import br.gov.quixada.sports.security.users.UserRole;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository repository;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(repository);
    }

    @Test
    @DisplayName("should return adapter when the email exists")
    void shouldReturnAdapter() {
        var user = new User("john", "john@example.com", "hash", UserRole.USER);
        when(repository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("john@example.com");

        assertThat(details).isInstanceOf(UserDetailsAdapter.class);
        assertThat(details.getUsername()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when the email does not exist")
    void shouldThrowWhenEmailDoesNotExist() {
        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
