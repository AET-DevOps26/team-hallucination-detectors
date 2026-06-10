package de.tum.devops.vibeshield.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack tests for the Websites contract: register, list, ownership isolation,
 * duplicate and validation errors — through the real JWT filter against H2+Flyway.
 */
@SpringBootTest(properties = "app.jwt.secret=" + TestTokens.SECRET)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebsiteApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebsiteRepository websiteRepository;

    @BeforeEach
    void cleanDatabase() {
        websiteRepository.deleteAll();
    }

    @Test
    void createWebsite_returns201WithDefaultedName() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/websites")
                        .header("Authorization", TestTokens.bearer("a@example.org", 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://shop.example.org\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("id").asLong()).isPositive();
        assertThat(body.get("url").asText()).isEqualTo("https://shop.example.org");
        assertThat(body.get("name").asText()).isEqualTo("shop.example.org");
        assertThat(body.has("createdAt")).isTrue();
    }

    @Test
    void createWebsite_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/websites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://shop.example.org\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void createWebsite_withInvalidUrl_returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/websites")
                        .header("Authorization", TestTokens.bearer("a@example.org", 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"ftp://shop.example.org\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createWebsite_withMissingUrl_returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/websites")
                        .header("Authorization", TestTokens.bearer("a@example.org", 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createWebsite_duplicateUrl_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/websites")
                        .header("Authorization", TestTokens.bearer("a@example.org", 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://shop.example.org\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/websites")
                        .header("Authorization", TestTokens.bearer("a@example.org", 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://shop.example.org\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WEBSITE_ALREADY_REGISTERED"));
    }

    @Test
    void listWebsites_onlyShowsOwnWebsites() throws Exception {
        mockMvc.perform(post("/api/v1/websites")
                        .header("Authorization", TestTokens.bearer("a@example.org", 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://a.example.org\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/websites")
                        .header("Authorization", TestTokens.bearer("b@example.org", 2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://b.example.org\"}"))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/v1/websites")
                        .header("Authorization", TestTokens.bearer("a@example.org", 1)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body).hasSize(1);
        assertThat(body.get(0).get("url").asText()).isEqualTo("https://a.example.org");
    }

    @Test
    void listWebsites_returnsNewestFirst() throws Exception {
        for (String host : new String[]{"first", "second", "third"}) {
            mockMvc.perform(post("/api/v1/websites")
                            .header("Authorization", TestTokens.bearer("a@example.org", 1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"url\": \"https://" + host + ".example.org\"}"))
                    .andExpect(status().isCreated());
        }

        MvcResult result = mockMvc.perform(get("/api/v1/websites")
                        .header("Authorization", TestTokens.bearer("a@example.org", 1)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body).hasSize(3);
        assertThat(body.get(0).get("url").asText()).contains("third");
        assertThat(body.get(2).get("url").asText()).contains("first");
    }
}
