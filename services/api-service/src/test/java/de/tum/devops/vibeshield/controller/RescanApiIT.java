package de.tum.devops.vibeshield.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.Severity;
import de.tum.devops.vibeshield.model.Finding;
import de.tum.devops.vibeshield.repository.FindingRepository;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration tests for Epic 6 rescan and current-vs-previous comparison behavior. */
@SpringBootTest(properties = "app.jwt.secret=" + TestTokens.SECRET)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RescanApiIT {

    private static final String OWNER = TestTokens.bearer("a@example.org", 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private WebsiteRepository websiteRepository;

    @BeforeEach
    void cleanDatabase() {
        findingRepository.deleteAll();
        scanRepository.deleteAll();
        websiteRepository.deleteAll();
    }

    @Test
    void rescan_createsPendingScanForSameWebsite() throws Exception {
        long scanId = completedScan(registerWebsite(),
                new FindingSpec(ScanCheck.HTTPS, "HTTP is reachable", Severity.HIGH, "http://shop.example.org"));

        MvcResult result = mockMvc.perform(post("/api/v1/scans/" + scanId + "/rescan")
                        .header("Authorization", OWNER))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("Pending"))
                .andReturn();

        long rescanId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(rescanId).isNotEqualTo(scanId);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/v1/scans/" + rescanId);
    }

    @Test
    void comparison_marksFixedStillPresentAndNewlyIntroducedFindings() throws Exception {
        long websiteId = registerWebsite();
        completedScan(websiteId,
                new FindingSpec(ScanCheck.HTTPS, "HTTP is reachable", Severity.HIGH, "http://shop.example.org"),
                new FindingSpec(ScanCheck.HEADERS, "Missing CSP", Severity.MEDIUM, "https://shop.example.org"));
        long currentScanId = completedScan(websiteId,
                new FindingSpec(ScanCheck.HEADERS, "Missing CSP", Severity.MEDIUM, "https://shop.example.org"),
                new FindingSpec(ScanCheck.SECRETS, "Exposed API token", Severity.CRITICAL, "main.js"));

        mockMvc.perform(get("/api/v1/scans/" + currentScanId + "/comparison")
                        .header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparable").value(true))
                .andExpect(jsonPath("$.summary.fixed").value(1))
                .andExpect(jsonPath("$.summary.stillPresent").value(1))
                .andExpect(jsonPath("$.summary.newlyIntroduced").value(1))
                .andExpect(jsonPath("$.findings[0].changeStatus").value("Newly introduced"))
                .andExpect(jsonPath("$.actionPlan[0].title").value("Exposed API token"))
                .andExpect(jsonPath("$.actionPlan[0].suggestedFixOrder").value(1));
    }

    @Test
    void comparison_withoutPreviousCompletedScan_isNotComparable() throws Exception {
        long scanId = completedScan(registerWebsite(),
                new FindingSpec(ScanCheck.HEADERS, "Missing CSP", Severity.MEDIUM, "https://shop.example.org"));

        mockMvc.perform(get("/api/v1/scans/" + scanId + "/comparison")
                        .header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comparable").value(false))
                .andExpect(jsonPath("$.message").value("No previous completed scan exists for this website yet."));
    }

    private long registerWebsite() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/websites")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://shop.example.org\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long completedScan(long websiteId, FindingSpec... findings) throws Exception {
        MvcResult triggered = mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checks\": [\"https\", \"headers\", \"secrets\"], \"crawlDepth\": 1}"))
                .andExpect(status().isAccepted())
                .andReturn();
        long scanId = objectMapper.readTree(triggered.getResponse().getContentAsString()).get("id").asLong();
        var scan = scanRepository.findById(scanId).orElseThrow();
        scan.markCompleted(Instant.now());
        scanRepository.save(scan);
        for (FindingSpec finding : findings) {
            findingRepository.save(new Finding(scanId, finding.check(), finding.title(), finding.severity(),
                    finding.affected(), "Explanation for " + finding.title(),
                    "Suggested fix for " + finding.title()));
        }
        return scanId;
    }

    private record FindingSpec(ScanCheck check, String title, Severity severity, String affected) {
    }
}
