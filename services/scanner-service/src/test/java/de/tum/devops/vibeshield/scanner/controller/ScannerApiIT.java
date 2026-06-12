package de.tum.devops.vibeshield.scanner.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end: the real HTTP stack scans a real (embedded JDK) web server that
 * deliberately misses security headers and exposes an .env file. Exercises the
 * contract endpoint, the fetcher, and the checks together — no mocks.
 *
 * <p>The embedded server lives on 127.0.0.1, so the SSRF guard's loopback rule is
 * relaxed for this test only (it stays strict in every deployed environment).
 */
@SpringBootTest(properties = "scanner.ssrf.allow-loopback=true")
@AutoConfigureMockMvc
class ScannerApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static HttpServer targetSite;
    private static String targetUrl;

    @BeforeAll
    static void startVulnerableTestSite() throws Exception {
        targetSite = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        targetSite.createContext("/", exchange -> {
            byte[] body = "<html><body>hi</body></html>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        targetSite.createContext("/.env", exchange -> {
            byte[] body = "DATABASE_URL=postgres://u:p@db/prod\nAPI_KEY=sk-test"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        targetSite.start();
        targetUrl = "http://127.0.0.1:" + targetSite.getAddress().getPort() + "/";
    }

    @AfterAll
    static void stopVulnerableTestSite() {
        targetSite.stop(0);
    }

    @Test
    void health_answersOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("vibeshield-scanner-service"));
    }

    @Test
    void scan_findsRealIssuesOnVulnerableSite() throws Exception {
        String request = """
                {"url": "%s", "checks": ["headers", "sensitiveFiles"]}
                """.formatted(targetUrl);

        MvcResult result = mockMvc.perform(post("/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Completed"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("pages")).hasSize(1);
        assertThat(body.get("executedChecks")).hasSize(2);

        List<String> titles = new ArrayList<>();
        body.get("findings").forEach(finding -> titles.add(finding.get("title").asText()));
        assertThat(titles)
                .anyMatch(title -> title.contains("Content-Security-Policy"))
                .anyMatch(title -> title.contains(".env"));

        body.get("findings").forEach(finding -> {
            assertThat(finding.get("explanation").asText()).isNotBlank();
            assertThat(finding.get("suggestedFix").asText()).isNotBlank();
        });
    }

    @Test
    void scan_ofUnreachableSite_reportsFailedWithReason() throws Exception {
        mockMvc.perform(post("/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        // RFC 5737 TEST-NET address — guaranteed unroutable.
                        .content("{\"url\": \"https://192.0.2.1/\", \"checks\": [\"https\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Failed"))
                .andExpect(jsonPath("$.errorMessage").isNotEmpty());
    }

    @Test
    void scan_ofPrivateAddress_isRefusedAsFailed() throws Exception {
        mockMvc.perform(post("/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        // RFC 1918 private range — the SSRF guard must refuse it, even
                        // though loopback is relaxed for this test.
                        .content("{\"url\": \"http://10.255.255.1/\", \"checks\": [\"headers\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Failed"))
                .andExpect(jsonPath("$.errorMessage", containsString("not allowed to reach")));
    }

    @Test
    void scan_withMalformedRequest_returns400() throws Exception {
        mockMvc.perform(post("/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checks\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
