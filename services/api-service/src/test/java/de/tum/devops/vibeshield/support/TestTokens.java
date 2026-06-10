package de.tum.devops.vibeshield.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Builds JWTs for integration tests the way the auth-service issues them:
 * subject = email, userId claim, HMAC-signed with the shared secret.
 */
public final class TestTokens {

    /** Secret the integration tests configure via {@code app.jwt.secret}. */
    public static final String SECRET = "test-secret-that-is-at-least-32-chars!!";

    private TestTokens() {
    }

    public static String token(String email, long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public static String bearer(String email, long userId) {
        return "Bearer " + token(email, userId);
    }
}
