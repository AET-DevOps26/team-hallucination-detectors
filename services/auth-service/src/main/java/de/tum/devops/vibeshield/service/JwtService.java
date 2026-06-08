package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and validates HMAC-signed JWTs that carry a user's identity between requests.
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret:dev-secret-key-change-me-minimum-32-chars!!}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    /** Builds a signed token whose subject is the user's email, plus a userId claim and expiry. */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /** Returns the subject (email) claim carried by the token. */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /** Returns true if the token's signature and expiry are valid, false otherwise. */
    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    /** Parses and verifies the token, returning its claims; throws if the token is invalid. */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Derives the HMAC signing key from the configured shared secret. */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
