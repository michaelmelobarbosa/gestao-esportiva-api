package br.gov.quixada.esporte.security.users;

import br.gov.quixada.esporte.security.auth.UserRepository;
import br.gov.quixada.esporte.security.users.exception.OperacaoNaoPermitidaException;
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
            throw new IllegalArgumentException("Senha deve ter entre 8 e 72 caracteres");
        }

        var user = repository.findByEmail(email)
                .filter(candidate -> candidate.getRole() == UserRole.SUPERADMIN)
                .orElseThrow(() -> new OperacaoNaoPermitidaException(
                        "SUPERADMIN não encontrado para o e-mail informado"));

        user.setPassword(passwordEncoder.encode(new String(newPassword)));
        user.setStatus(UserStatus.ATIVO);
        repository.saveAndFlush(user);

        auditService.recordSystem(user, UserAuditAction.PASSWORD_RESET,
                "senha redefinida via CLI de recuperação");
    }
}
