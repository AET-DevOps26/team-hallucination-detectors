package de.tum.devops.vibeshield.scanner.checks;

import de.tum.devops.vibeshield.scanner.generated.model.ScanCheck;
import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.http.SiteFetcher;

import java.net.URI;
import java.util.List;

/**
 * One surface-level security check. Implementations are stateless; the executor
 * passes the per-scan fetcher (which carries the request budget) into each run.
 */
public interface SecurityCheck {

    /** Which {@code ScanCheck} option this implementation covers. */
    ScanCheck type();

    /** Runs the check against the target and returns zero or more findings. */
    List<ScannerFinding> run(URI target, SiteFetcher fetcher);
}
