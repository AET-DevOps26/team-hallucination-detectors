package de.tum.devops.vibeshield.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end filter behaviour through the servlet chain: public paths stay open,
 * protected API paths reject missing/invalid tokens with the unified 401 error body,
 * and valid tokens expose the caller's identity to controllers.
 */
@SpringBootTest(properties = "app.jwt.secret=" + JwtAuthFilterTest.SECRET)
@AutoConfigureMockMvc
class JwtAuthFilterTest {

    static final String SECRET = "test-secret-that-is-at-least-32-chars!!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test-only protected endpoint that echoes the identity the filter extracted.
     * The nested controller is registered exactly once through @TestConfiguration
     * member-class processing — do not add an explicit @Bean for it.
     */
    @TestConfiguration
    static class ProbeConfig {

        @RestController
        static class ProbeController {
            @GetMapping("/api/v1/whoami-probe")
            Map<String, Object> whoami(@RequestAttribute(JwtAuthFilter.USER_ATTRIBUTE) AuthenticatedUser user) {
                return Map.of("userId", user.userId(), "email", user.email());
            }
        }
    }

    private String token(String email, long userId, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void publicHelloEndpoint_needsNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/hello"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocs_needNoToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withoutToken_returnsUnifiedUnauthorizedError() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/whoami-probe"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(body.has("message")).isTrue();
    }

    @Test
    void protectedEndpoint_withGarbageToken_returnsInvalidTokenError() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/whoami-probe")
                        .header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void protectedEndpoint_withExpiredToken_returnsInvalidTokenError() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/whoami-probe")
                        .header("Authorization", "Bearer " + token("dev@vibeshield.dev", 7, -60_000)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void protectedEndpoint_withValidToken_seesCallerIdentity() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/whoami-probe")
                        .header("Authorization", "Bearer " + token("dev@vibeshield.dev", 7, 60_000)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("userId").asLong()).isEqualTo(7L);
        assertThat(body.get("email").asText()).isEqualTo("dev@vibeshield.dev");
    }
}
