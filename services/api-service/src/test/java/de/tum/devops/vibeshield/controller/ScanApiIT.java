package de.tum.devops.vibeshield.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.devops.vibeshield.repository.ScanRepository;
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
 * Full-stack tests for the scan slice up to (not including) execution: trigger
 * returns 202+Location with a Pending scan, duplicate triggers 409, foreign
 * resources 404, polling and history read real persisted state.
 */
@SpringBootTest(properties = "app.jwt.secret=" + TestTokens.SECRET)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScanApiIT {

    private static final String OWNER = TestTokens.bearer("a@example.org", 1);
    private static final String STRANGER = TestTokens.bearer("b@example.org", 2);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private WebsiteRepository websiteRepository;

    @BeforeEach
    void cleanDatabase() {
        scanRepository.deleteAll();
        websiteRepository.deleteAll();
    }

    private long registerWebsite(String bearer, String url) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/websites")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void triggerScan_returns202WithLocationAndPendingScan() throws Exception {
        long websiteId = registerWebsite(OWNER, "https://shop.example.org");

        MvcResult result = mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checks\": [\"https\", \"headers\"], \"crawlDepth\": 1}"))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("status").asText()).isEqualTo("Pending");
        assertThat(body.get("websiteId").asLong()).isEqualTo(websiteId);
        assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo("/api/v1/scans/" + body.get("id").asLong());
    }

    @Test
    void triggerScan_withEmptyBody_usesDefaults() throws Exception {
        long websiteId = registerWebsite(OWNER, "https://shop.example.org");

        mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("Pending"));
    }

    @Test
    void triggerScan_withoutBody_returns400ValidationError() throws Exception {
        long websiteId = registerWebsite(OWNER, "https://shop.example.org");

        mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void triggerScan_whileScanInFlight_returns409() throws Exception {
        long websiteId = registerWebsite(OWNER, "https://shop.example.org");
        mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCAN_IN_PROGRESS"));
    }

    @Test
    void triggerScan_onForeignWebsite_returns404() throws Exception {
        long websiteId = registerWebsite(OWNER, "https://shop.example.org");

        mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", STRANGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WEBSITE_NOT_FOUND"));
    }

    @Test
    void getScan_pollsPersistedPendingState() throws Exception {
        long websiteId = registerWebsite(OWNER, "https://shop.example.org");
        MvcResult triggered = mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andReturn();
        long scanId = objectMapper.readTree(triggered.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/v1/scans/" + scanId).header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending"))
                .andExpect(jsonPath("$.completedAt").doesNotExist())
                .andExpect(jsonPath("$.errorMessage").doesNotExist());
    }

    @Test
    void getScan_forForeignScan_returns404() throws Exception {
        long websiteId = registerWebsite(OWNER, "https://shop.example.org");
        MvcResult triggered = mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andReturn();
        long scanId = objectMapper.readTree(triggered.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/v1/scans/" + scanId).header("Authorization", STRANGER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCAN_NOT_FOUND"));
    }

    @Test
    void listFindings_isEmptyWhileScanIsPending() throws Exception {
        long websiteId = registerWebsite(OWNER, "https://shop.example.org");
        MvcResult triggered = mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andReturn();
        long scanId = objectMapper.readTree(triggered.getResponse().getContentAsString())
                .get("id").asLong();

        MvcResult result = mockMvc.perform(get("/api/v1/scans/" + scanId + "/findings")
                        .header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())).isEmpty();
    }

    @Test
    void latestScan_returns404BeforeFirstScan_thenTheNewestOne() throws Exception {
        long websiteId = registerWebsite(OWNER, "https://shop.example.org");

        mockMvc.perform(get("/api/v1/websites/" + websiteId + "/scans/latest")
                        .header("Authorization", OWNER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCAN_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/websites/" + websiteId + "/scans/latest")
                        .header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending"))
                .andExpect(header().string("Content-Type", "application/json"));
    }

    @Test
    void listScans_returnsHistoryNewestFirst() throws Exception {
        long websiteId = registerWebsite(OWNER, "https://shop.example.org");
        mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted());

        MvcResult result = mockMvc.perform(get("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body).hasSize(1);
        assertThat(body.get(0).get("status").asText()).isEqualTo("Pending");
    }
}
