package de.tum.devops.vibeshield.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for token validation: accepts tokens signed with the shared secret,
 * rejects expired, foreign-signed, malformed, and empty tokens.
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-chars!!";
    private static final String OTHER_SECRET = "another-secret-that-is-32-chars-long!!!";

    private final JwtService jwtService = new JwtService(SECRET);

    /** Builds a token the way the auth-service does: subject = email, userId claim, expiry. */
    private String token(String secret, String email, long userId, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void validToken_returnsCarriedIdentity() {
        AuthenticatedUser user = jwtService.authenticate(token(SECRET, "dev@vibeshield.dev", 7L, 60_000));

        assertThat(user.userId()).isEqualTo(7L);
        assertThat(user.email()).isEqualTo("dev@vibeshield.dev");
    }

    @Test
    void expiredToken_isRejected() {
        String expired = token(SECRET, "dev@vibeshield.dev", 7L, -60_000);

        assertThatThrownBy(() -> jwtService.authenticate(expired))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithDifferentSecret_isRejected() {
        String foreign = token(OTHER_SECRET, "dev@vibeshield.dev", 7L, 60_000);

        assertThatThrownBy(() -> jwtService.authenticate(foreign))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void malformedToken_isRejected() {
        assertThatThrownBy(() -> jwtService.authenticate("not.a.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void emptyToken_isRejected() {
        assertThatThrownBy(() -> jwtService.authenticate(""))
                .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }
}
