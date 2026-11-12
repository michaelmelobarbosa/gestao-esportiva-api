package br.gov.quixada.sports.security.users;

import br.gov.quixada.sports.security.auth.UserRepository;
import br.gov.quixada.sports.security.auth.exception.EmailAlreadyRegisteredException;
import br.gov.quixada.sports.security.users.dto.UserCreateRequest;
import br.gov.quixada.sports.security.users.dto.UserUpdateRequest;
import br.gov.quixada.sports.security.users.exception.OperationNotAllowedException;
import br.gov.quixada.sports.security.users.exception.UserNotFoundException;
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
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    public User create(User actor, UserCreateRequest request) {
        validateSuperadmin(actor);

        if (request.role() == UserRole.SUPERADMIN) {
            throw new OperationNotAllowedException("Creating a SUPERADMIN through the API is not allowed");
        }
        if (repository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        var user = new User(request.username(), request.email(),
                passwordEncoder.encode(request.password()), request.role());
        user.setStatus(UserStatus.ACTIVE);

        var saved = saveTraduzindoDuplicity(user);
        auditService.record(actor, saved, UserAuditAction.CREATE, "role: " + saved.getRole());
        return saved;
    }

    @Transactional
    public User update(User actor, Long id, UserUpdateRequest request) {
        validateSuperadmin(actor);

        var user = findByIdOrThrowNotFound(id);
        boolean self = user.getId().equals(actor.getId());

        if (self) {
            if (request.role() != user.getRole()) {
                throw new OperationNotAllowedException("Changing your own role is not allowed");
            }
        } else {
            if (user.getRole() == UserRole.SUPERADMIN) {
                throw new OperationNotAllowedException("Changing another SUPERADMIN is not allowed");
            }
            if (request.role() == UserRole.SUPERADMIN) {
                throw new OperationNotAllowedException("Promoting a user to SUPERADMIN is not allowed");
            }
        }

        if (!user.getEmail().equalsIgnoreCase(request.email()) && repository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        var alteracoes = describeChanges(user, request);
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setRole(request.role());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        var saved = saveTraduzindoDuplicity(user);
        auditService.record(actor, saved, UserAuditAction.UPDATE, alteracoes);
        return saved;
    }

    @Transactional
    public User changeStatus(User actor, Long id, UserStatus status) {
        validateSuperadmin(actor);

        var user = findByIdOrThrowNotFound(id);

        if (user.getId().equals(actor.getId())) {
            throw new OperationNotAllowedException("Changing your own status is not allowed");
        }
        if (user.getRole() == UserRole.SUPERADMIN) {
            throw new OperationNotAllowedException("Deactivating a SUPERADMIN is not allowed");
        }
        if (user.getStatus() == status) {
            return user;
        }

        var previousStatus = user.getStatus();
        user.setStatus(status);
        auditService.record(actor, user, UserAuditAction.STATUS_CHANGE,
                "status: " + previousStatus + " -> " + status);
        return user;
    }

    private void validateSuperadmin(User actor) {
        if (actor.getRole() != UserRole.SUPERADMIN) {
            throw new OperationNotAllowedException("Only SUPERADMIN can manage users");
        }
    }

    private String describeChanges(User user, UserUpdateRequest request) {
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
            alteracoes.add("password: changed");
        }

        return alteracoes.isEmpty() ? "no changes" : String.join("; ", alteracoes);
    }

    private User saveTraduzindoDuplicity(User user) {
        try {
            return repository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            var message = e.getMostSpecificCause().getMessage();
            if (message != null && message.contains("uk_db_users_email")) {
                throw new EmailAlreadyRegisteredException("Email already registered");
            }
            throw e;
        }
    }
}
