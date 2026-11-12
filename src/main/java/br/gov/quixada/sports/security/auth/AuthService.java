package br.gov.quixada.sports.security.auth;

import br.gov.quixada.sports.security.auth.exception.EmailAlreadyRegisteredException;
import br.gov.quixada.sports.security.users.User;
import br.gov.quixada.sports.security.users.UserRole;
import br.gov.quixada.sports.security.users.UserStatus;
import br.gov.quixada.sports.security.users.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        var user = new User(request.username(), request.email(),
                passwordEncoder.encode(request.password()), UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw traduzirViolacaoDeUnicity(e);
        }
    }

    private RuntimeException traduzirViolacaoDeUnicity(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();

        if (message != null && message.contains("uk_db_users_email")) {
            return new EmailAlreadyRegisteredException("Email already registered");
        }
        return e;
    }
}
