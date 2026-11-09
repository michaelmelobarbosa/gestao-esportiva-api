package br.gov.quixada.esporte.security.auth;

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

import br.gov.quixada.esporte.security.users.User;
import br.gov.quixada.esporte.security.users.UserDetailsAdapter;
import br.gov.quixada.esporte.security.users.UserRole;

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
    @DisplayName("deve retornar adapter quando o email existir")
    void deveRetornarAdapter() {
        var user = new User("fulano", "fulano@example.com", "hash", UserRole.USER);
        when(repository.findByEmail("fulano@example.com")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("fulano@example.com");

        assertThat(details).isInstanceOf(UserDetailsAdapter.class);
        assertThat(details.getUsername()).isEqualTo("fulano@example.com");
    }

    @Test
    @DisplayName("deve lançar UsernameNotFoundException quando o email não existir")
    void deveLancarQuandoNaoExistir() {
        when(repository.findByEmail("nao@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nao@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
