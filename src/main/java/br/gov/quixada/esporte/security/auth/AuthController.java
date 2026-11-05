package br.gov.quixada.esporte.security.auth;

import br.gov.quixada.esporte.security.config.JwtService;
import br.gov.quixada.esporte.security.users.UserDetailsAdapter;
import br.gov.quixada.esporte.security.users.dto.LoginRequest;
import br.gov.quixada.esporte.security.users.dto.LoginResponse;
import br.gov.quixada.esporte.security.users.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping(path = "/v{version}/auth", version = "1.0")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var auth = authenticationManager.authenticate(usernamePassword);

        var userDetails = (UserDetailsAdapter) Objects.requireNonNull(auth.getPrincipal());
        var token = jwtService.generateToken(userDetails.user());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }
}
