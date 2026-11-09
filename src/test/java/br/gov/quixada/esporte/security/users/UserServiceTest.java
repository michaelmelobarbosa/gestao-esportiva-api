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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.gov.quixada.esporte.security.auth.UserRepository;
import br.gov.quixada.esporte.security.users.dto.UserCreateRequest;
import br.gov.quixada.esporte.security.users.dto.UserUpdateRequest;
import br.gov.quixada.esporte.security.users.exception.OperacaoNaoPermitidaException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserAuditService auditService;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, passwordEncoder, auditService);
    }

    private User user(Long id, UserRole role, UserStatus status) {
        var user = new User("usuario" + id, "usuario" + id + "@example.com", "hash", role);
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    private UserUpdateRequest updateRequest(String email, String password, UserRole role) {
        return new UserUpdateRequest("usuario", email, password, role);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("deve permitir SUPERADMIN criar ADMIN")
        void devePermitirSuperadminCriarAdmin() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ATIVO);
            var request = new UserCreateRequest("novo", "novo@example.com", "senha123", UserRole.ADMIN);
            when(repository.existsByEmail("novo@example.com")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("hash");
            when(repository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            var saved = service.create(actor, request);

            assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
            verify(auditService).record(actor, saved, UserAuditAction.CREATE, "role: ADMIN");
        }

        @Test
        @DisplayName("deve proibir criação de SUPERADMIN pela API")
        void deveProibirCriarSuperadmin() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ATIVO);
            var request = new UserCreateRequest("novo", "novo@example.com", "senha123", UserRole.SUPERADMIN);

            assertThatThrownBy(() -> service.create(actor, request))
                    .isInstanceOf(OperacaoNaoPermitidaException.class)
                    .hasMessage("Não é permitido criar SUPERADMIN pela API");

            verify(repository, never()).saveAndFlush(any(User.class));
        }

        @Test
        @DisplayName("deve proibir ADMIN de gerenciar usuários")
        void deveProibirAdminCriar() {
            var actor = user(1L, UserRole.ADMIN, UserStatus.ATIVO);
            var request = new UserCreateRequest("novo", "novo@example.com", "senha123", UserRole.USER);

            assertThatThrownBy(() -> service.create(actor, request))
                    .isInstanceOf(OperacaoNaoPermitidaException.class);

            verify(repository, never()).saveAndFlush(any(User.class));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("deve permitir SUPERADMIN editar ADMIN")
        void devePermitirSuperadminEditarAdmin() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ATIVO);
            var target = user(2L, UserRole.ADMIN, UserStatus.ATIVO);
            when(repository.findById(2L)).thenReturn(Optional.of(target));
            when(repository.saveAndFlush(target)).thenReturn(target);

            var request = updateRequest(target.getEmail(), null, UserRole.ADMIN);
            var saved = service.update(actor, 2L, request);

            assertThat(saved.getUsername()).isEqualTo("usuario");
            verify(auditService).record(actor, saved, UserAuditAction.UPDATE, "username: usuario2 -> usuario");
        }

        @Test
        @DisplayName("deve proibir alteração do próprio role")
        void deveProibirAlterarProprioRole() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ATIVO);
            var request = updateRequest(actor.getEmail(), null, UserRole.ADMIN);
            when(repository.findById(1L)).thenReturn(Optional.of(actor));

            assertThatThrownBy(() -> service.update(actor, 1L, request))
                    .isInstanceOf(OperacaoNaoPermitidaException.class)
                    .hasMessage("Não é permitido alterar o próprio perfil de acesso");

            verify(repository, never()).saveAndFlush(any(User.class));
        }

        @Test
        @DisplayName("deve permitir ao SUPERADMIN editar o próprio perfil mantendo o role")
        void devePermitirEditarProprioPerfil() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(actor));
            when(repository.saveAndFlush(actor)).thenReturn(actor);

            var request = updateRequest(actor.getEmail(), null, UserRole.SUPERADMIN);
            service.update(actor, 1L, request);

            verify(auditService).record(actor, actor, UserAuditAction.UPDATE, "username: usuario1 -> usuario");
        }

        @Test
        @DisplayName("deve proibir editar outro SUPERADMIN")
        void deveProibirEditarOutroSuperadmin() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ATIVO);
            var target = user(2L, UserRole.SUPERADMIN, UserStatus.ATIVO);
            when(repository.findById(2L)).thenReturn(Optional.of(target));

            var request = updateRequest(target.getEmail(), null, UserRole.SUPERADMIN);

            assertThatThrownBy(() -> service.update(actor, 2L, request))
                    .isInstanceOf(OperacaoNaoPermitidaException.class)
                    .hasMessage("Não é permitido alterar outro SUPERADMIN");

            verify(repository, never()).saveAndFlush(any(User.class));
        }

        @Test
        @DisplayName("deve proibir ADMIN de editar usuários")
        void deveProibirAdminEditar() {
            var actor = user(1L, UserRole.ADMIN, UserStatus.ATIVO);
            var request = updateRequest("outro@example.com", null, UserRole.ADMIN);

            assertThatThrownBy(() -> service.update(actor, 2L, request))
                    .isInstanceOf(OperacaoNaoPermitidaException.class)
                    .hasMessage("Apenas SUPERADMIN pode gerenciar usuários");
        }
    }

    @Nested
    @DisplayName("alterarStatus")
    class AlterarStatus {

        @Test
        @DisplayName("deve permitir SUPERADMIN inativar usuário comum")
        void devePermitirInativarUsuario() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ATIVO);
            var target = user(2L, UserRole.USER, UserStatus.ATIVO);
            when(repository.findById(2L)).thenReturn(Optional.of(target));

            var saved = service.alterarStatus(actor, 2L, UserStatus.INATIVO);

            assertThat(saved.getStatus()).isEqualTo(UserStatus.INATIVO);
            verify(auditService).record(actor, target, UserAuditAction.STATUS_CHANGE,
                    "status: ATIVO -> INATIVO");
        }

        @Test
        @DisplayName("deve proibir alteração do próprio status")
        void deveProibirAlterarProprioStatus() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ATIVO);
            when(repository.findById(1L)).thenReturn(Optional.of(actor));

            assertThatThrownBy(() -> service.alterarStatus(actor, 1L, UserStatus.INATIVO))
                    .isInstanceOf(OperacaoNaoPermitidaException.class)
                    .hasMessage("Não é permitido alterar o próprio status");
        }

        @Test
        @DisplayName("deve proibir inativar SUPERADMIN")
        void deveProibirInativarSuperadmin() {
            var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ATIVO);
            var target = user(2L, UserRole.SUPERADMIN, UserStatus.ATIVO);
            when(repository.findById(2L)).thenReturn(Optional.of(target));

            assertThatThrownBy(() -> service.alterarStatus(actor, 2L, UserStatus.INATIVO))
                    .isInstanceOf(OperacaoNaoPermitidaException.class)
                    .hasMessage("Não é permitido inativar um SUPERADMIN");
        }
    }

    @Test
    @DisplayName("não deve auditar alteração de status quando o status é o mesmo")
    void naoDeveAuditarStatusIgual() {
        var actor = user(1L, UserRole.SUPERADMIN, UserStatus.ATIVO);
        var target = user(2L, UserRole.USER, UserStatus.ATIVO);
        when(repository.findById(2L)).thenReturn(Optional.of(target));

        service.alterarStatus(actor, 2L, UserStatus.ATIVO);

        verify(auditService, never()).record(any(), any(), any(), anyString());
    }
}
