package br.gov.quixada.esporte.security.auth;

import br.gov.quixada.esporte.security.auth.exception.EmailJaCadastradoException;
import br.gov.quixada.esporte.security.users.User;
import br.gov.quixada.esporte.security.users.UserRole;
import br.gov.quixada.esporte.security.users.dto.RegisterRequest;
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
            throw new EmailJaCadastradoException("Email já cadastrado");
        }

        var user = new User(request.username(), request.email(),
                passwordEncoder.encode(request.password()), UserRole.USER);

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw traduzirViolacaoDeUnicidade(e);
        }
    }

    private RuntimeException traduzirViolacaoDeUnicidade(DataIntegrityViolationException e) {
        String mensagem = e.getMostSpecificCause().getMessage();

        if (mensagem != null && mensagem.contains("uk_db_users_email")) {
            return new EmailJaCadastradoException("Email já cadastrado");
        }
        return e;
    }
}
