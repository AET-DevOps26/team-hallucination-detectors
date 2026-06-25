package de.tum.devops.vibeshield.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration tests for computed report exports. */
@SpringBootTest(properties = "app.jwt.secret=" + TestTokens.SECRET)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportApiIT {

    private static final String OWNER = TestTokens.bearer("a@example.org", 1);
    private static final String STRANGER = TestTokens.bearer("b@example.org", 2);

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
    void reportData_returnsPrioritizedFindingsChecklistAndNextSteps() throws Exception {
        long scanId = completedScanWithFindings();

        MvcResult result = mockMvc.perform(get("/api/v1/scans/" + scanId + "/report/data")
                        .header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanId").value(scanId))
                .andExpect(jsonPath("$.safeToLaunch.status").value("Not safe to launch"))
                .andExpect(jsonPath("$.executiveSummary.riskLevel").value("Critical"))
                .andExpect(jsonPath("$.executiveSummary.recommendedNextSteps[0].title")
                        .value("Exposed API token"))
                .andExpect(jsonPath("$.safeToLaunch.items[2].label").value("HTTPS and mixed-content checks"))
                .andExpect(jsonPath("$.safeToLaunch.items[2].result").value("Pass"))
                .andExpect(jsonPath("$.safeToLaunch.items[4].label").value("Public admin and login paths"))
                .andExpect(jsonPath("$.safeToLaunch.items[4].result").value("Not selected"))
                .andExpect(jsonPath("$.safeToLaunch.items[5].label").value("Client bundle secrets"))
                .andExpect(jsonPath("$.safeToLaunch.items[5].result").value("Needs attention"))
                .andExpect(jsonPath("$.findings[0].suggestedFixOrder").value(1))
                .andExpect(jsonPath("$.findings[0].effort.level").value("High"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("safeToLaunch").get("items")).hasSize(7);
    }

    @Test
    void reportData_forForeignScan_returns404() throws Exception {
        long scanId = completedScanWithFindings();

        mockMvc.perform(get("/api/v1/scans/" + scanId + "/report/data")
                        .header("Authorization", STRANGER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCAN_NOT_FOUND"));
    }

    @Test
    void reportPdf_returnsDownloadablePdf() throws Exception {
        long scanId = completedScanWithFindings();

        MvcResult result = mockMvc.perform(get("/api/v1/scans/" + scanId + "/report/full.pdf")
                        .header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"vibeshield-scan-" + scanId + "-full.pdf\""))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray())
                .startsWith("%PDF-1.4".getBytes());
    }

    private long completedScanWithFindings() throws Exception {
        long websiteId = registerWebsite();
        MvcResult triggered = mockMvc.perform(post("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checks\": [\"https\", \"headers\", \"secrets\"]}"))
                .andExpect(status().isAccepted())
                .andReturn();
        long scanId = objectMapper.readTree(triggered.getResponse().getContentAsString())
                .get("id").asLong();

        var scan = scanRepository.findById(scanId).orElseThrow();
        scan.markCompleted(Instant.now());
        scanRepository.save(scan);

        findingRepository.save(new Finding(scanId, ScanCheck.HEADERS, "Missing HSTS header",
                Severity.HIGH, "https://shop.example.org", "The site does not force secure browser connections.",
                "Add Strict-Transport-Security with a long max-age."));
        findingRepository.save(new Finding(scanId, ScanCheck.SECRETS, "Exposed API token",
                Severity.CRITICAL, "main.js", "A client bundle appears to expose a token.",
                "Rotate the token and move it to server-side configuration."));
        return scanId;
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
}
