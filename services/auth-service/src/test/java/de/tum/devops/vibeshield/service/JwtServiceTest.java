package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtService} — pure token logic, no Spring context and no DB.
 * The {@code @Value} fields are populated directly via reflection so the service can be
 * exercised in isolation.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-minimum-32-characters-long!!";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
    }

    /** Builds a User with an id set, since the id is normally assigned by JPA. */
    private User user(long id, String email) {
        User user = new User(email, "irrelevant-password-hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    // U1
    @Test
    void generateToken_thenExtractEmail_roundTrips() {
        String token = jwtService.generateToken(user(1L, "alice@example.com"));

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("alice@example.com");
    }

    // U2
    @Test
    void generateToken_carriesUserIdClaim() {
        String token = jwtService.generateToken(user(42L, "alice@example.com"));

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();

        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(42L);
    }

    // U3
    @Test
    void isTokenValid_freshToken_isTrue() {
        String token = jwtService.generateToken(user(1L, "alice@example.com"));

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    // U4
    @Test
    void isTokenValid_garbageString_isFalse() {
        assertThat(jwtService.isTokenValid("this.is.not-a-jwt")).isFalse();
    }

    // U5
    @Test
    void isTokenValid_tokenSignedWithDifferentSecret_isFalse() {
        JwtService otherIssuer = new JwtService();
        ReflectionTestUtils.setField(otherIssuer, "jwtSecret",
                "a-completely-different-secret-key-also-32+chars!!");
        ReflectionTestUtils.setField(otherIssuer, "expirationMs", 3_600_000L);

        String foreignToken = otherIssuer.generateToken(user(1L, "alice@example.com"));

        assertThat(jwtService.isTokenValid(foreignToken)).isFalse();
    }

    // U6
    @Test
    void isTokenValid_expiredToken_isFalse() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1_000L);
        String expiredToken = jwtService.generateToken(user(1L, "alice@example.com"));

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }
}
