package de.tum.devops.vibeshield.scanner;

import de.tum.devops.vibeshield.scannerclient.model.ScanExecutionRequest;
import de.tum.devops.vibeshield.scannerclient.model.ScanExecutionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * HTTP client for the internal scanner contract (api/scanner-internal.yaml).
 * Request/response types are generated from that contract — the wire format can
 * only change by changing the spec. A scan execution can legitimately take a
 * while (many probes against a slow site), hence the generous read timeout.
 */
@Component
public class ScannerClient {

    private final RestClient restClient;

    public ScannerClient(@Value("${scanner.base-url:http://scanner-service:8080}") String baseUrl,
                         @Value("${scanner.read-timeout-ms:180000}") long readTimeoutMs) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    /** Liveness probe (#28); false means "do not dispatch scans right now". */
    public boolean isHealthy() {
        try {
            restClient.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * Executes one scan synchronously.
     *
     * @throws org.springframework.web.client.RestClientException on transport or protocol errors
     */
    public ScanExecutionResult execute(ScanExecutionRequest request) {
        return restClient.post()
                .uri("/scan")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ScanExecutionResult.class);
    }
}
