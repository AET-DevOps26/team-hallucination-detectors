package de.tum.devops.vibeshield.scanner.service;

import de.tum.devops.vibeshield.scanner.checks.SecurityCheck;
import de.tum.devops.vibeshield.scanner.generated.model.ScanCheck;
import de.tum.devops.vibeshield.scanner.generated.model.ScanExecutionRequest;
import de.tum.devops.vibeshield.scanner.generated.model.ScanExecutionResult;
import de.tum.devops.vibeshield.scanner.generated.model.ScannerFinding;
import de.tum.devops.vibeshield.scanner.generated.model.Severity;
import de.tum.devops.vibeshield.scanner.http.BlockedAddressException;
import de.tum.devops.vibeshield.scanner.http.RequestBudgetExceededException;
import de.tum.devops.vibeshield.scanner.http.SiteFetcher;
import de.tum.devops.vibeshield.scanner.support.FakeSiteFetcher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for check orchestration: which checks run, failure isolation, budget/SSRF handling. */
class ScanExecutorTest {

    private static final URI TARGET = URI.create("https://shop.example.org/");

    private SecurityCheck checkFor(ScanCheck type, List<ScannerFinding> findings) {
        SecurityCheck check = mock(SecurityCheck.class);
        when(check.type()).thenReturn(type);
        when(check.run(any(), any())).thenReturn(findings);
        return check;
    }

    private ScannerFinding finding(ScanCheck check, String title) {
        return new ScannerFinding(check, title, Severity.MEDIUM, "https://shop.example.org/", "explanation", "fix");
    }

    private ScanExecutionRequest request(ScanCheck... checks) {
        return new ScanExecutionRequest(TARGET, List.of(checks));
    }

    private FetcherFactory factoryReturning(SiteFetcher fetcher) {
        FetcherFactory factory = mock(FetcherFactory.class);
        when(factory.newFetcher()).thenReturn(fetcher);
        return factory;
    }

    @Test
    void execute_returnsFailed_whenTargetIsUnreachable() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher(); // no scripted response -> unreachable
        ScanExecutor executor = new ScanExecutor(List.of(), factoryReturning(fetcher), mock(PageDiscovery.class), new SimpleMeterRegistry());

        ScanExecutionResult result = executor.execute(request(ScanCheck.HTTPS));

        assertThat(result.getStatus()).isEqualTo(ScanExecutionResult.StatusEnum.FAILED);
        assertThat(result.getErrorMessage()).contains(TARGET.toString());
        assertThat(result.getExecutedChecks()).isEmpty();
        assertThat(result.getPages()).isEmpty();
        assertThat(result.getFindings()).isEmpty();
    }

    @Test
    void execute_returnsFailed_whenTargetIsBlockedBySsrfGuard() {
        SiteFetcher fetcher = mock(SiteFetcher.class);
        when(fetcher.fetch(TARGET)).thenThrow(new BlockedAddressException("Target resolves to a private address."));
        ScanExecutor executor = new ScanExecutor(List.of(), factoryReturning(fetcher), mock(PageDiscovery.class), new SimpleMeterRegistry());

        ScanExecutionResult result = executor.execute(request(ScanCheck.HTTPS));

        assertThat(result.getStatus()).isEqualTo(ScanExecutionResult.StatusEnum.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Target resolves to a private address.");
    }

    @Test
    void execute_onlyRunsChecksRequestedForTheScan() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher().respond(TARGET.toString(), 200, java.util.Map.of(), "<html/>");
        SecurityCheck https = checkFor(ScanCheck.HTTPS, List.of());
        SecurityCheck headers = checkFor(ScanCheck.HEADERS, List.of());
        ScanExecutor executor = new ScanExecutor(List.of(https, headers), factoryReturning(fetcher), mock(PageDiscovery.class), new SimpleMeterRegistry());

        executor.execute(request(ScanCheck.HTTPS)); // headers not requested

        verify(https).run(any(), any());
        verify(headers, never()).run(any(), any());
    }

    @Test
    void execute_aggregatesFindingsFromEveryRequestedCheckAndListsThemExecuted() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher().respond(TARGET.toString(), 200, java.util.Map.of(), "<html/>");
        SecurityCheck https = checkFor(ScanCheck.HTTPS, List.of(finding(ScanCheck.HTTPS, "No HTTPS")));
        SecurityCheck headers = checkFor(ScanCheck.HEADERS, List.of(finding(ScanCheck.HEADERS, "Missing CSP")));
        ScanExecutor executor = new ScanExecutor(List.of(https, headers), factoryReturning(fetcher), mock(PageDiscovery.class), new SimpleMeterRegistry());

        ScanExecutionResult result = executor.execute(request(ScanCheck.HTTPS, ScanCheck.HEADERS));

        assertThat(result.getStatus()).isEqualTo(ScanExecutionResult.StatusEnum.COMPLETED);
        assertThat(result.getExecutedChecks()).containsExactly(ScanCheck.HTTPS, ScanCheck.HEADERS);
        assertThat(result.getFindings()).extracting(ScannerFinding::getTitle)
                .containsExactly("No HTTPS", "Missing CSP");
        assertThat(result.getPages()).containsExactly(TARGET);
    }

    @Test
    void execute_isolatesOneFailingCheckWithoutStoppingTheOthers() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher().respond(TARGET.toString(), 200, java.util.Map.of(), "<html/>");
        SecurityCheck broken = mock(SecurityCheck.class);
        when(broken.type()).thenReturn(ScanCheck.HTTPS);
        when(broken.run(any(), any())).thenThrow(new RuntimeException("boom"));
        SecurityCheck healthy = checkFor(ScanCheck.HEADERS, List.of(finding(ScanCheck.HEADERS, "Missing CSP")));
        ScanExecutor executor = new ScanExecutor(List.of(broken, healthy), factoryReturning(fetcher), mock(PageDiscovery.class), new SimpleMeterRegistry());

        ScanExecutionResult result = executor.execute(request(ScanCheck.HTTPS, ScanCheck.HEADERS));

        assertThat(result.getStatus()).isEqualTo(ScanExecutionResult.StatusEnum.COMPLETED);
        assertThat(result.getExecutedChecks()).containsExactly(ScanCheck.HEADERS);
        assertThat(result.getFindings()).extracting(ScannerFinding::getTitle).containsExactly("Missing CSP");
    }

    @Test
    void execute_stopsRunningFurtherChecksOnceTheRequestBudgetIsExhausted() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher().respond(TARGET.toString(), 200, java.util.Map.of(), "<html/>");
        SecurityCheck first = checkFor(ScanCheck.HTTPS, List.of(finding(ScanCheck.HTTPS, "No HTTPS")));
        SecurityCheck exhausting = mock(SecurityCheck.class);
        when(exhausting.type()).thenReturn(ScanCheck.HEADERS);
        when(exhausting.run(any(), any())).thenThrow(new RequestBudgetExceededException(10));
        SecurityCheck neverRun = checkFor(ScanCheck.SENSITIVE_FILES, List.of());
        ScanExecutor executor = new ScanExecutor(List.of(first, exhausting, neverRun),
                factoryReturning(fetcher), mock(PageDiscovery.class), new SimpleMeterRegistry());

        ScanExecutionResult result = executor.execute(
                request(ScanCheck.HTTPS, ScanCheck.HEADERS, ScanCheck.SENSITIVE_FILES));

        assertThat(result.getStatus()).isEqualTo(ScanExecutionResult.StatusEnum.COMPLETED);
        assertThat(result.getExecutedChecks()).containsExactly(ScanCheck.HTTPS);
        verify(neverRun, never()).run(any(), any());
    }
}
