package br.gov.quixada.esporte.security.auth;

import br.gov.quixada.esporte.security.JwtService;
import br.gov.quixada.esporte.security.auth.dto.LoginRequest;
import br.gov.quixada.esporte.security.auth.dto.LoginResponse;
import br.gov.quixada.esporte.security.auth.dto.RegisterRequest;
import br.gov.quixada.esporte.security.users.User;
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

import java.util.Objects;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(request.username(), request.password());
        var auth = authenticationManager.authenticate(usernamePassword);

        var token = jwtService.generateToken((User) Objects.requireNonNull(auth.getPrincipal()));
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {

        if (this.userRepository.findByUsername(request.username()) != null) {
            return ResponseEntity.badRequest().build();
        }

        var encryptedPassword = passwordEncoder.encode(request.password());
        var user = new User(request.username(), encryptedPassword, request.role());

        this.userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
