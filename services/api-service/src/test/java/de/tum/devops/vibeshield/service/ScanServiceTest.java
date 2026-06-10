package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.exception.ConflictException;
import de.tum.devops.vibeshield.exception.NotFoundException;
import de.tum.devops.vibeshield.generated.model.ScanCheck;
import de.tum.devops.vibeshield.generated.model.ScanRequest;
import de.tum.devops.vibeshield.generated.model.ScanStatus;
import de.tum.devops.vibeshield.model.Scan;
import de.tum.devops.vibeshield.model.Website;
import de.tum.devops.vibeshield.repository.FindingRepository;
import de.tum.devops.vibeshield.repository.ScanRepository;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Unit tests for the trigger rules: ownership, duplicate guard, request defaults. */
@ExtendWith(MockitoExtension.class)
class ScanServiceTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(7L, "dev@vibeshield.dev");
    private static final Website WEBSITE =
            new Website(7L, "https://shop.example.org", "Shop", Instant.now());

    @Mock
    private ScanRepository scanRepository;
    @Mock
    private FindingRepository findingRepository;
    @Mock
    private WebsiteRepository websiteRepository;

    private ScanService service;

    @BeforeEach
    void setUp() {
        service = new ScanService(scanRepository, findingRepository, websiteRepository);
    }

    @Test
    void trigger_createsPendingScanWithAllChecksByDefault() {
        when(websiteRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(WEBSITE));
        when(scanRepository.existsByWebsiteIdAndStatusIn(eq(1L), anyCollection())).thenReturn(false);
        when(scanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Scan scan = service.trigger(USER, 1L, null);

        assertThat(scan.getStatus()).isEqualTo(ScanStatus.PENDING);
        assertThat(scan.getRequestedChecks()).containsExactly(ScanCheck.values());
        assertThat(scan.getCrawlDepth()).isZero();
        assertThat(scan.isIncludeSubdomains()).isFalse();
    }

    @Test
    void trigger_respectsExplicitScanConfiguration() {
        when(websiteRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(WEBSITE));
        when(scanRepository.existsByWebsiteIdAndStatusIn(eq(1L), anyCollection())).thenReturn(false);
        when(scanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ScanRequest request = new ScanRequest()
                .checks(List.of(ScanCheck.HTTPS, ScanCheck.HEADERS))
                .crawlDepth(2)
                .includeSubdomains(true);
        Scan scan = service.trigger(USER, 1L, request);

        assertThat(scan.getRequestedChecks()).containsExactly(ScanCheck.HTTPS, ScanCheck.HEADERS);
        assertThat(scan.getCrawlDepth()).isEqualTo(2);
        assertThat(scan.isIncludeSubdomains()).isTrue();
    }

    @Test
    void trigger_rejectsWhenScanAlreadyInFlight() {
        when(websiteRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(WEBSITE));
        when(scanRepository.existsByWebsiteIdAndStatusIn(eq(1L), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.trigger(USER, 1L, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void trigger_answers404ForForeignWebsite() {
        when(websiteRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.trigger(USER, 1L, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getScan_answers404ForForeignScan() {
        Scan foreignScan = new Scan(99L, List.of(ScanCheck.HTTPS), 0, false, Instant.now());
        when(scanRepository.findById(5L)).thenReturn(Optional.of(foreignScan));
        when(websiteRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getScan(USER, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Scan not found");
    }

    @Test
    void getLatestScan_answers404WhenWebsiteHasNoScans() {
        when(websiteRepository.findByIdAndOwnerId(1L, 7L)).thenReturn(Optional.of(WEBSITE));
        when(scanRepository.findFirstByWebsiteIdOrderByCreatedAtDescIdDesc(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatestScan(USER, 1L))
                .isInstanceOf(NotFoundException.class);
    }
}
