package br.gov.quixada.sports.security.auth;

import br.gov.quixada.sports.security.auth.exception.TooManyAttemptsException;
import br.gov.quixada.sports.security.jwt.JwtService;
import br.gov.quixada.sports.security.jwt.RefreshTokenService;
import br.gov.quixada.sports.security.users.UserDetailsAdapter;
import br.gov.quixada.sports.security.users.dto.LoginRequest;
import br.gov.quixada.sports.security.users.dto.LoginResponse;
import br.gov.quixada.sports.security.users.dto.RefreshTokenRequest;
import br.gov.quixada.sports.security.users.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Objects;

@RestController
@RequestMapping(path = "/v{version}/auth", version = "1.0")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh, and logout")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;

    @Operation(summary = "Authenticate user and issue JWT and refresh token")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                               HttpServletRequest httpRequest) {
        var ip = httpRequest.getRemoteAddr();

        var waitTime = loginRateLimiter.attempt(ip, request.email());
        if (waitTime.isPresent()) {
            throw new TooManyAttemptsException(toSeconds(waitTime.get()));
        }

        var usernamePassword = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var auth = authenticationManager.authenticate(usernamePassword);
        loginRateLimiter.returnAttempt(ip, request.email());

        var userDetails = (UserDetailsAdapter) Objects.requireNonNull(auth.getPrincipal());
        var token = jwtService.generateToken(userDetails.user());
        var refreshToken = refreshTokenService.issue(userDetails.user());
        return ResponseEntity.ok(new LoginResponse(token, refreshToken));
    }

    @Operation(summary = "Refresh access token using a valid refresh token")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        var rotated = refreshTokenService.rotate(request.refreshToken());
        var accessToken = jwtService.generateToken(rotated.user());
        return ResponseEntity.ok(new LoginResponse(accessToken, rotated.refreshToken()));
    }

    @Operation(summary = "Logout user and revoke refresh tokens")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetailsAdapter principal) {
        authService.logout(principal.user().getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Register a new user")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    private static long toSeconds(Duration waitTime) {
        return Math.max(1L, (long) Math.ceil(waitTime.toNanos() / 1_000_000_000.0));
    }
}
