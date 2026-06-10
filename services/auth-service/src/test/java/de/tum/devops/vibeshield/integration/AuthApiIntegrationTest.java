package de.tum.devops.vibeshield.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.devops.vibeshield.model.User;
import de.tum.devops.vibeshield.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the auth API: the full application context (real
 * controller, repository, JWT service, BCrypt encoder) over an in-memory H2 database,
 * driven through real HTTP requests via MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    private ResultActions register(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    // I4
    @Test
    void register_newEmail_succeedsAndStoresHashedPassword() throws Exception {
        register("new@example.com", "s3cret-password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        User saved = userRepository.findByEmail("new@example.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("s3cret-password");
        assertThat(saved.getPassword()).startsWith("$2a$"); // BCrypt hash marker
    }

    // I5
    @Test
    void register_duplicateEmail_returns400() throws Exception {
        register("dupe@example.com", "pw").andExpect(status().isOk());

        register("dupe@example.com", "another-pw")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    // I6
    @Test
    void login_unknownEmail_returns401() throws Exception {
        login("ghost@example.com", "pw")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // I7
    @Test
    void login_wrongPassword_returns401() throws Exception {
        register("user@example.com", "correct-password").andExpect(status().isOk());

        login("user@example.com", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // I8
    @Test
    void login_validCredentials_returnsTokenAndEmail() throws Exception {
        register("user@example.com", "correct-password").andExpect(status().isOk());

        login("user@example.com", "correct-password")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    // I9
    @Test
    void me_missingAuthorizationHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // I10
    @Test
    void me_malformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    // I11
    @Test
    void fullFlow_registerThenLoginThenMe_returnsEmail() throws Exception {
        register("flow@example.com", "pw").andExpect(status().isOk());

        String loginBody = login("flow@example.com", "pw")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginBody).get("token").asText();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow@example.com"));
    }
}
