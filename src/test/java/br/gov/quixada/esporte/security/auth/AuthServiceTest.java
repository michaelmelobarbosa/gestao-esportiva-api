package br.gov.quixada.esporte.security.auth;

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

import br.gov.quixada.esporte.security.auth.exception.EmailJaCadastradoException;
import br.gov.quixada.esporte.security.users.User;
import br.gov.quixada.esporte.security.users.UserRole;
import br.gov.quixada.esporte.security.users.dto.RegisterRequest;

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

    private RegisterRequest umRequest() {
        return new RegisterRequest("fulano", "fulano@example.com", "senha123");
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("deve cadastrar usuário com senha criptografada e role USER")
        void deveCadastrarUsuario() {
            when(userRepository.existsByEmail("fulano@example.com")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("hash");

            service.register(umRequest());

            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User saved = captor.getValue();
            assertThat(saved.getUsername()).isEqualTo("fulano");
            assertThat(saved.getEmail()).isEqualTo("fulano@example.com");
            assertThat(saved.getPassword()).isEqualTo("hash");
            assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("deve permitir username duplicado")
        void devePermitirUsernameDuplicado() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");

            service.register(umRequest());
            service.register(umRequest());

            verify(userRepository, times(2)).save(any(User.class));
        }

        @Test
        @DisplayName("deve lançar EmailJaCadastradoException quando email já existir")
        void deveLancarQuandoEmailJaCadastrado() {
            when(userRepository.existsByEmail("fulano@example.com")).thenReturn(true);

            assertThatThrownBy(() -> service.register(umRequest()))
                    .isInstanceOf(EmailJaCadastradoException.class)
                    .hasMessage("Email já cadastrado");

            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("deve converter violação do índice de email em EmailJaCadastradoException")
        void deveConverterViolacaoDeEmail() {
            when(userRepository.existsByEmail("fulano@example.com")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("hash");
            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException(
                            "Duplicate entry 'fulano@example.com' for key 'db_users.uk_db_users_email'"));

            assertThatThrownBy(() -> service.register(umRequest()))
                    .isInstanceOf(EmailJaCadastradoException.class)
                    .hasMessage("Email já cadastrado");
        }

        @Test
        @DisplayName("deve relançar violação de integridade desconhecida")
        void deveRelancarViolacaoDesconhecida() {
            when(userRepository.existsByEmail("fulano@example.com")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("hash");
            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("outra violação"));

            assertThatThrownBy(() -> service.register(umRequest()))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessage("outra violação");
        }
    }
}
