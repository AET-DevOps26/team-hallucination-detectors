package de.tum.devops.vibeshield.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validates HMAC-signed JWTs issued by the auth-service. Validation-only counterpart
 * of the auth-service's JwtService: this service never issues tokens, it only verifies
 * them against the shared signing secret and extracts the caller's identity.
 *
 * <p>The shared-secret (symmetric) scheme is the documented MVP seam — see
 * {@code docs/auth.md}. A later switch to asymmetric keys only has to replace this class.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${app.jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verifies the token's signature and expiry and returns the identity it carries.
     *
     * @throws JwtException             if the token is malformed, expired, or signed with a different key
     * @throws IllegalArgumentException if the token is empty
     */
    public AuthenticatedUser authenticate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Number userId = claims.get("userId", Number.class);
        return new AuthenticatedUser(userId == null ? null : userId.longValue(), claims.getSubject());
    }
}
