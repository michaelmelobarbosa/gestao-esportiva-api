package br.gov.quixada.esporte.security.users;

import br.gov.quixada.esporte.security.auth.UserRepository;
import br.gov.quixada.esporte.security.auth.exception.EmailJaCadastradoException;
import br.gov.quixada.esporte.security.users.dto.UserCreateRequest;
import br.gov.quixada.esporte.security.users.dto.UserUpdateRequest;
import br.gov.quixada.esporte.security.users.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

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
    public User create(UserCreateRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new EmailJaCadastradoException("Email já cadastrado");
        }

        var user = new User(request.username(), request.email(),
                passwordEncoder.encode(request.password()), request.role());
        user.setStatus(UserStatus.ATIVO);
        return salvarTraduzindoDuplicidade(user);
    }

    @Transactional
    public User update(Long id, UserUpdateRequest request) {
        var user = findByIdOrThrowNotFound(id);

        if (!user.getEmail().equalsIgnoreCase(request.email()) && repository.existsByEmail(request.email())) {
            throw new EmailJaCadastradoException("Email já cadastrado");
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setRole(request.role());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return salvarTraduzindoDuplicidade(user);
    }

    @Transactional
    public User alterarStatus(Long id, UserStatus status) {
        var user = findByIdOrThrowNotFound(id);
        user.setStatus(status);
        return user;
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
