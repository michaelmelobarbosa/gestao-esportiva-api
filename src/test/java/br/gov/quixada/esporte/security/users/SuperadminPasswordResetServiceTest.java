package br.gov.quixada.esporte.security.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.gov.quixada.esporte.security.auth.UserRepository;
import br.gov.quixada.esporte.security.users.exception.OperacaoNaoPermitidaException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuperadminPasswordResetService")
class SuperadminPasswordResetServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserAuditService auditService;

    private SuperadminPasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new SuperadminPasswordResetService(repository, passwordEncoder, auditService);
    }

    private User user(UserRole role, UserStatus status) {
        var user = new User("admin", "admin@quixada.gov.br", "hash-antigo", role);
        user.setId(1L);
        user.setStatus(status);
        return user;
    }

    @Test
    @DisplayName("deve redefinir a senha do SUPERADMIN, reativar e auditar")
    void deveRedefinirSenhaDoSuperadmin() {
        var superadmin = user(UserRole.SUPERADMIN, UserStatus.INATIVO);
        when(repository.findByEmail("admin@quixada.gov.br")).thenReturn(Optional.of(superadmin));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hash-novo");

        service.reset("admin@quixada.gov.br", "novaSenha123".toCharArray());

        assertThat(superadmin.getPassword()).isEqualTo("hash-novo");
        assertThat(superadmin.getStatus()).isEqualTo(UserStatus.ATIVO);
        verify(repository).saveAndFlush(superadmin);
        verify(auditService).recordSystem(superadmin, UserAuditAction.PASSWORD_RESET,
                "senha redefinida via CLI de recuperação");
    }

    @Test
    @DisplayName("deve falhar quando o e-mail não existe")
    void deveFalharQuandoEmailNaoExiste() {
        when(repository.findByEmail("naoexiste@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reset("naoexiste@example.com", "novaSenha123".toCharArray()))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessage("SUPERADMIN não encontrado para o e-mail informado");

        verify(repository, never()).saveAndFlush(any(User.class));
        verify(auditService, never()).recordSystem(any(), any(), anyString());
    }

    @Test
    @DisplayName("deve falhar quando o usuário não é SUPERADMIN")
    void deveFalharQuandoUsuarioNaoEhSuperadmin() {
        var admin = user(UserRole.ADMIN, UserStatus.ATIVO);
        when(repository.findByEmail("admin@quixada.gov.br")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.reset("admin@quixada.gov.br", "novaSenha123".toCharArray()))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessage("SUPERADMIN não encontrado para o e-mail informado");

        verify(repository, never()).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("deve rejeitar senha fora do intervalo permitido")
    void deveRejeitarSenhaForaDoIntervalo() {
        assertThatThrownBy(() -> service.reset("admin@quixada.gov.br", "curta".toCharArray()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Senha deve ter entre 8 e 72 caracteres");

        verify(repository, never()).findByEmail(anyString());
    }
}
