package br.gov.quixada.esporte.security.users;

import br.gov.quixada.esporte.security.auth.UserRepository;
import br.gov.quixada.esporte.security.auth.exception.EmailJaCadastradoException;
import br.gov.quixada.esporte.security.users.dto.UserCreateRequest;
import br.gov.quixada.esporte.security.users.dto.UserUpdateRequest;
import br.gov.quixada.esporte.security.users.exception.OperacaoNaoPermitidaException;
import br.gov.quixada.esporte.security.users.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuditService auditService;

    @Transactional(readOnly = true)
    public Page<User> findAll(String search, UserStatus status, Pageable pageable) {
        return repository.findAll(UserSpecifications.withFilters(search, status), pageable);
    }

    @Transactional(readOnly = true)
    public User findByIdOrThrowNotFound(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
    }

    @Transactional
    public User create(User actor, UserCreateRequest request) {
        validarSuperadmin(actor);

        if (request.role() == UserRole.SUPERADMIN) {
            throw new OperacaoNaoPermitidaException("Não é permitido criar SUPERADMIN pela API");
        }
        if (repository.existsByEmail(request.email())) {
            throw new EmailJaCadastradoException("Email já cadastrado");
        }

        var user = new User(request.username(), request.email(),
                passwordEncoder.encode(request.password()), request.role());
        user.setStatus(UserStatus.ATIVO);

        var saved = salvarTraduzindoDuplicidade(user);
        auditService.record(actor, saved, UserAuditAction.CREATE, "role: " + saved.getRole());
        return saved;
    }

    @Transactional
    public User update(User actor, Long id, UserUpdateRequest request) {
        validarSuperadmin(actor);

        var user = findByIdOrThrowNotFound(id);
        boolean self = user.getId().equals(actor.getId());

        if (self) {
            if (request.role() != user.getRole()) {
                throw new OperacaoNaoPermitidaException("Não é permitido alterar o próprio perfil de acesso");
            }
        } else {
            if (user.getRole() == UserRole.SUPERADMIN) {
                throw new OperacaoNaoPermitidaException("Não é permitido alterar outro SUPERADMIN");
            }
            if (request.role() == UserRole.SUPERADMIN) {
                throw new OperacaoNaoPermitidaException("Não é permitido promover usuário para SUPERADMIN");
            }
        }

        if (!user.getEmail().equalsIgnoreCase(request.email()) && repository.existsByEmail(request.email())) {
            throw new EmailJaCadastradoException("Email já cadastrado");
        }

        var alteracoes = descreverAlteracoes(user, request);
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setRole(request.role());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        var saved = salvarTraduzindoDuplicidade(user);
        auditService.record(actor, saved, UserAuditAction.UPDATE, alteracoes);
        return saved;
    }

    @Transactional
    public User alterarStatus(User actor, Long id, UserStatus status) {
        validarSuperadmin(actor);

        var user = findByIdOrThrowNotFound(id);

        if (user.getId().equals(actor.getId())) {
            throw new OperacaoNaoPermitidaException("Não é permitido alterar o próprio status");
        }
        if (user.getRole() == UserRole.SUPERADMIN) {
            throw new OperacaoNaoPermitidaException("Não é permitido inativar um SUPERADMIN");
        }
        if (user.getStatus() == status) {
            return user;
        }

        var statusAnterior = user.getStatus();
        user.setStatus(status);
        auditService.record(actor, user, UserAuditAction.STATUS_CHANGE,
                "status: " + statusAnterior + " -> " + status);
        return user;
    }

    private void validarSuperadmin(User actor) {
        if (actor.getRole() != UserRole.SUPERADMIN) {
            throw new OperacaoNaoPermitidaException("Apenas SUPERADMIN pode gerenciar usuários");
        }
    }

    private String descreverAlteracoes(User user, UserUpdateRequest request) {
        var alteracoes = new ArrayList<String>();

        if (!user.getUsername().equals(request.username())) {
            alteracoes.add("username: " + user.getUsername() + " -> " + request.username());
        }
        if (!user.getEmail().equalsIgnoreCase(request.email())) {
            alteracoes.add("email: " + user.getEmail() + " -> " + request.email());
        }
        if (user.getRole() != request.role()) {
            alteracoes.add("role: " + user.getRole() + " -> " + request.role());
        }
        if (request.password() != null && !request.password().isBlank()) {
            alteracoes.add("password: alterada");
        }

        return alteracoes.isEmpty() ? "sem alterações" : String.join("; ", alteracoes);
    }

    private User salvarTraduzindoDuplicidade(User user) {
        try {
            return repository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            var mensagem = e.getMostSpecificCause().getMessage();
            if (mensagem != null && mensagem.contains("uk_db_users_email")) {
                throw new EmailJaCadastradoException("Email já cadastrado");
            }
            throw e;
        }
    }
}
