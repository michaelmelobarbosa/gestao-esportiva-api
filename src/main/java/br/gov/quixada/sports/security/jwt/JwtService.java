package br.gov.quixada.sports.security.jwt;

import br.gov.quixada.sports.security.auth.exception.InvalidTokenException;
import br.gov.quixada.sports.security.users.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final TokenProperties properties;
    private final Clock clock;

    public record TokenClaims(String email, int tokenVersion) {
    }

    public String generateToken(User user) {

        try {

            var algorithm = Algorithm.HMAC256(properties.secret());

            return JWT.create().withIssuer(properties.issuer())
                    .withSubject(user.getEmail())
                    .withClaim("ver", user.getTokenVersion())
                    .withClaim("role", user.getRole().name())
                    .withExpiresAt(expirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error while generating token", exception);
        }
    }

    public TokenClaims extractClaims(String token) {

        try {

            var algorithm = Algorithm.HMAC256(properties.secret());

            var decoded = JWT.require(algorithm)
                    .withIssuer(properties.issuer())
                    .build()
                    .verify(token);

            String subject = decoded.getSubject();
            Integer ver = decoded.getClaim("ver").asInt();
            if (subject == null || ver == null) {
                throw new InvalidTokenException("Invalid or expired token");
            }
            return new TokenClaims(subject, ver);
        } catch (JWTVerificationException exception) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    public String extractEmail(String token) {
        return extractClaims(token).email();
    }


    private Instant expirationDate() {
        return clock.instant().plus(properties.expiration());
    }

}
