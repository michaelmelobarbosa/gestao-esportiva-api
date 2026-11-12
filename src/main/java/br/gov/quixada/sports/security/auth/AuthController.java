package br.gov.quixada.sports.security.auth;

import br.gov.quixada.sports.security.auth.exception.TooManyAttemptsException;
import br.gov.quixada.sports.security.jwt.JwtService;
import br.gov.quixada.sports.security.users.UserDetailsAdapter;
import br.gov.quixada.sports.security.users.dto.LoginRequest;
import br.gov.quixada.sports.security.users.dto.LoginResponse;
import br.gov.quixada.sports.security.users.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Objects;

@RestController
@RequestMapping(path = "/v{version}/auth", version = "1.0")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                               HttpServletRequest httpRequest) {
        var ip = httpRequest.getRemoteAddr();

        var waitTime = loginRateLimiter.attempt(ip, request.email());
        if (waitTime.isPresent()) {
            throw new TooManyAttemptsException(paraSegundos(waitTime.get()));
        }

        var usernamePassword = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var auth = authenticationManager.authenticate(usernamePassword);
        loginRateLimiter.returnAttempt(ip, request.email());

        var userDetails = (UserDetailsAdapter) Objects.requireNonNull(auth.getPrincipal());
        var token = jwtService.generateToken(userDetails.user());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    private static long paraSegundos(Duration waitTime) {
        return Math.max(1L, (long) Math.ceil(waitTime.toNanos() / 1_000_000_000.0));
    }
}
