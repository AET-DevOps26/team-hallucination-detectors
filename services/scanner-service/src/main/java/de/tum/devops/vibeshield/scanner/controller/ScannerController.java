package de.tum.devops.vibeshield.scanner.controller;

import de.tum.devops.vibeshield.scanner.generated.api.ScannerApi;
import de.tum.devops.vibeshield.scanner.generated.model.HealthResponse;
import de.tum.devops.vibeshield.scanner.generated.model.ScanExecutionRequest;
import de.tum.devops.vibeshield.scanner.generated.model.ScanExecutionResult;
import de.tum.devops.vibeshield.scanner.service.ScanExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the generated internal contract (api/scanner-internal.yaml): one
 * synchronous scan execution endpoint (#27) plus the liveness check the backend
 * uses before dispatching work (#28).
 */
@RestController
public class ScannerController implements ScannerApi {

    private final ScanExecutor scanExecutor;

    public ScannerController(ScanExecutor scanExecutor) {
        this.scanExecutor = scanExecutor;
    }

    @Override
    public ResponseEntity<ScanExecutionResult> executeScan(ScanExecutionRequest scanExecutionRequest) {
        return ResponseEntity.ok(scanExecutor.execute(scanExecutionRequest));
    }

    @Override
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(new HealthResponse()
                .status("ok")
                .service("vibeshield-scanner-service"));
    }
}
