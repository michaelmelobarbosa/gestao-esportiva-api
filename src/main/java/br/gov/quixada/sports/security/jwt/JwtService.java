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

    public String generateToken(User user) {

        try {

            var algorithm = Algorithm.HMAC256(properties.secret());

            return JWT.create().withIssuer(properties.issuer())
                    .withSubject(user.getEmail())
                    .withExpiresAt(expirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error while generating token", exception);
        }
    }

    public String extractEmail(String token) {

        try {

            var algorithm = Algorithm.HMAC256(properties.secret());

            return JWT.require(algorithm)
                    .withIssuer(properties.issuer())
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }


    private Instant expirationDate() {
        return clock.instant().plus(properties.expiration());
    }

}
