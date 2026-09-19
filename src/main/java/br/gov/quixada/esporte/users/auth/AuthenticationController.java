package br.gov.quixada.esporte.users.auth;

import br.gov.quixada.esporte.users.User;
import br.gov.quixada.esporte.users.dto.AuthenticationRequest;
import br.gov.quixada.esporte.users.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationManager authenticationManager;
    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid AuthenticationRequest request) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(request.userName(), request.password());
        authenticationManager.authenticate(usernamePassword);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {

        if (this.authenticationRepository.findByUsername(request.username()) != null) {
            return ResponseEntity.badRequest().build();
        }

        String encryptedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.username(), encryptedPassword, request.role());

        this.authenticationRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
