package br.gov.quixada.sports.security.users;

import br.gov.quixada.sports.security.auth.UserRepository;
import br.gov.quixada.sports.security.users.exception.OperationNotAllowedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SuperadminPasswordResetService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuditService auditService;

    @Transactional
    public void reset(String email, char[] newPassword) {
        if (newPassword == null || newPassword.length < 8 || newPassword.length > 72) {
            throw new IllegalArgumentException("Password must be between 8 and 72 characters");
        }

        var user = repository.findByEmail(email)
                .filter(candidate -> candidate.getRole() == UserRole.SUPERADMIN)
                .orElseThrow(() -> new OperationNotAllowedException(
                        "SUPERADMIN not found for the informed email"));

        user.setPassword(passwordEncoder.encode(new String(newPassword)));
        user.setStatus(UserStatus.ACTIVE);
        repository.saveAndFlush(user);

        auditService.recordSystem(user, UserAuditAction.PASSWORD_RESET,
                "password reset via recovery CLI");
    }
}
