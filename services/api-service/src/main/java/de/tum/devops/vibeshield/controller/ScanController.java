package de.tum.devops.vibeshield.controller;

import de.tum.devops.vibeshield.generated.api.ScansApi;
import de.tum.devops.vibeshield.generated.model.Finding;
import de.tum.devops.vibeshield.generated.model.Scan;
import de.tum.devops.vibeshield.generated.model.ScanRequest;
import de.tum.devops.vibeshield.mapper.ScanMapper;
import de.tum.devops.vibeshield.security.CurrentUser;
import de.tum.devops.vibeshield.service.ScanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Implements the generated Scans contract (api/openapi.yaml). The trigger answers
 * 202 immediately with a Location header for polling; execution happens in the
 * background worker, never in the request thread.
 */
@RestController
public class ScanController implements ScansApi {

    private final ScanService scanService;
    private final CurrentUser currentUser;

    public ScanController(ScanService scanService, CurrentUser currentUser) {
        this.scanService = scanService;
        this.currentUser = currentUser;
    }

    @Override
    public ResponseEntity<Scan> triggerScan(Long websiteId, ScanRequest scanRequest) {
        var scan = scanService.trigger(currentUser.require(), websiteId, scanRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(URI.create("/api/v1/scans/" + scan.getId()))
                .body(ScanMapper.toModel(scan));
    }

    @Override
    public ResponseEntity<Scan> getScan(Long scanId) {
        return ResponseEntity.ok(ScanMapper.toModel(scanService.getScan(currentUser.require(), scanId)));
    }

    @Override
    public ResponseEntity<List<Scan>> listScans(Long websiteId) {
        List<Scan> scans = scanService.listScans(currentUser.require(), websiteId).stream()
                .map(ScanMapper::toModel)
                .toList();
        return ResponseEntity.ok(scans);
    }

    @Override
    public ResponseEntity<Scan> getLatestScan(Long websiteId) {
        return ResponseEntity.ok(
                ScanMapper.toModel(scanService.getLatestScan(currentUser.require(), websiteId)));
    }

    @Override
    public ResponseEntity<List<Finding>> listFindings(Long scanId) {
        List<Finding> findings = scanService.listFindings(currentUser.require(), scanId).stream()
                .map(ScanMapper::toModel)
                .toList();
        return ResponseEntity.ok(findings);
    }
}
