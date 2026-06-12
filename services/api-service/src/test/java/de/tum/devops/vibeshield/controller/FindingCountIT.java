package de.tum.devops.vibeshield.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.model.Scan;
import de.tum.devops.vibeshield.model.Website;
import de.tum.devops.vibeshield.repository.FindingRepository;
import de.tum.devops.vibeshield.repository.ScanRepository;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.scannerclient.model.ScannerFinding;
import de.tum.devops.vibeshield.scannerclient.model.Severity;
import de.tum.devops.vibeshield.service.ScanProcessingService;
import de.tum.devops.vibeshield.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The contract's findingCount field (#21): null while a scan is Pending, the real
 * count once the worker completed it — on single-scan, latest, and history reads.
 */
@SpringBootTest(properties = "app.jwt.secret=" + TestTokens.SECRET)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FindingCountIT {

    private static final String OWNER = TestTokens.bearer("a@example.org", 1);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WebsiteRepository websiteRepository;
    @Autowired
    private ScanRepository scanRepository;
    @Autowired
    private FindingRepository findingRepository;
    @Autowired
    private ScanProcessingService processingService;

    private long websiteId;
    private long scanId;

    @BeforeEach
    void seedCompletedScan() {
        findingRepository.deleteAll();
        scanRepository.deleteAll();
        websiteRepository.deleteAll();

        Website website = websiteRepository.save(
                new Website(1L, "https://shop.example.org", "Shop", Instant.now()));
        websiteId = website.getId();
        Scan scan = scanRepository.save(new Scan(websiteId,
                List.of(ScanCheck.HEADERS), 0, false, Instant.now()));
        scanId = scan.getId();
    }

    private void completeWithTwoFindings() {
        processingService.claim(scanId);
        processingService.complete(scanId, List.of(
                new ScannerFinding()
                        .check(de.tum.devops.vibeshield.scannerclient.model.ScanCheck.HEADERS)
                        .title("Missing Content-Security-Policy header")
                        .severity(Severity.MEDIUM)
                        .affected("https://shop.example.org/")
                        .explanation("explanation")
                        .suggestedFix("fix"),
                new ScannerFinding()
                        .check(de.tum.devops.vibeshield.scannerclient.model.ScanCheck.HEADERS)
                        .title("Missing X-Content-Type-Options header")
                        .severity(Severity.LOW)
                        .affected("https://shop.example.org/")
                        .explanation("explanation")
                        .suggestedFix("fix")));
    }

    @Test
    void pendingScan_hasNoFindingCount() throws Exception {
        mockMvc.perform(get("/api/v1/scans/" + scanId).header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending"))
                .andExpect(jsonPath("$.findingCount").doesNotExist());
    }

    @Test
    void completedScan_reportsFindingCountEverywhere() throws Exception {
        completeWithTwoFindings();

        mockMvc.perform(get("/api/v1/scans/" + scanId).header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Completed"))
                .andExpect(jsonPath("$.findingCount").value(2));

        mockMvc.perform(get("/api/v1/websites/" + websiteId + "/scans/latest")
                        .header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findingCount").value(2));

        MvcResult history = mockMvc.perform(get("/api/v1/websites/" + websiteId + "/scans")
                        .header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(history.getResponse().getContentAsString());
        assertThat(body).hasSize(1);
        assertThat(body.get(0).get("findingCount").asInt()).isEqualTo(2);
    }

    @Test
    void completedScan_findingsAreSortedMostSevereFirst() throws Exception {
        completeWithTwoFindings();

        MvcResult result = mockMvc.perform(get("/api/v1/scans/" + scanId + "/findings")
                        .header("Authorization", OWNER))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode findings = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(findings).hasSize(2);
        assertThat(findings.get(0).get("severity").asText()).isEqualTo("Medium");
        assertThat(findings.get(1).get("severity").asText()).isEqualTo("Low");
        assertThat(findings.get(0).get("status").asText()).isEqualTo("Open");
    }
}
