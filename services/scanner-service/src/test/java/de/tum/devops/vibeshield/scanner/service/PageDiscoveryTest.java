package de.tum.devops.vibeshield.scanner.service;

import de.tum.devops.vibeshield.scanner.http.FetchResult;
import de.tum.devops.vibeshield.scanner.http.RequestBudgetExceededException;
import de.tum.devops.vibeshield.scanner.http.SiteFetcher;
import de.tum.devops.vibeshield.scanner.support.FakeSiteFetcher;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PageDiscoveryTest {

    private final PageDiscovery discovery = new PageDiscovery();
    private static final URI TARGET = URI.create("https://shop.example.org/");

    @Test
    void followsSameOriginLinksThatResolve() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "<html><body>"
                                + "<a href=\"/about\">About</a>"
                                + "<a href=\"/pricing\">Pricing</a>"
                                + "</body></html>")
                .respond("https://shop.example.org/about", 200, Map.of(), "About us")
                .respond("https://shop.example.org/pricing", 200, Map.of(), "Pricing");

        List<URI> pages = discovery.discover(TARGET, fetcher);

        assertThat(pages).containsExactlyInAnyOrder(
                TARGET,
                URI.create("https://shop.example.org/about"),
                URI.create("https://shop.example.org/pricing"));
    }

    @Test
    void ignoresCrossOriginLinks() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "<a href=\"https://cdn.example.net/about\">About</a>")
                // Scripted so that IF the check followed it, a page would appear —
                // its absence proves the cross-origin link was never followed.
                .respond("https://cdn.example.net/about", 200, Map.of(), "About us");

        assertThat(discovery.discover(TARGET, fetcher)).containsExactly(TARGET);
    }

    @Test
    void ignoresNonPageSchemesAndFragments() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "<a href=\"mailto:hi@example.org\">Mail</a>"
                                + "<a href=\"tel:+1234567890\">Call</a>"
                                + "<a href=\"javascript:void(0)\">JS</a>"
                                + "<a href=\"#section\">Jump</a>");

        assertThat(discovery.discover(TARGET, fetcher)).containsExactly(TARGET);
    }

    @Test
    void doesNotReportLinksThatDoNotResolve() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "<a href=\"/dead-link\">Dead</a><a href=\"/moved\">Moved</a>")
                .respond("https://shop.example.org/dead-link", 404, Map.of(), "not found")
                .respond("https://shop.example.org/moved", 302, Map.of("Location", "/new"), "");

        assertThat(discovery.discover(TARGET, fetcher)).containsExactly(TARGET);
    }

    @Test
    void dedupesRepeatedLinks() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .respond("https://shop.example.org/", 200, Map.of(),
                        "<a href=\"/about\">About (header)</a><a href=\"/about\">About (footer)</a>")
                .respond("https://shop.example.org/about", 200, Map.of(), "About us");

        assertThat(discovery.discover(TARGET, fetcher))
                .containsExactly(TARGET, URI.create("https://shop.example.org/about"));
    }

    @Test
    void capsHowManyLinksAreFollowed() {
        StringBuilder html = new StringBuilder("<html><body>");
        FakeSiteFetcher fetcher = new FakeSiteFetcher();
        for (int i = 1; i <= 10; i++) {
            html.append("<a href=\"/page").append(i).append("\">Page ").append(i).append("</a>");
            fetcher.respond("https://shop.example.org/page" + i, 200, Map.of(), "Page " + i);
        }
        html.append("</body></html>");
        fetcher.respond("https://shop.example.org/", 200, Map.of(), html.toString());

        // 1 (target) + at most 4 discovered pages, never all 10 links on the page.
        assertThat(discovery.discover(TARGET, fetcher)).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    void unreachableHomepage_reportsOnlyTheTarget() {
        FakeSiteFetcher fetcher = new FakeSiteFetcher()
                .unreachable("https://shop.example.org/");

        assertThat(discovery.discover(TARGET, fetcher)).containsExactly(TARGET);
    }

    @Test
    void exhaustedBudgetDuringDiscovery_returnsWhateverWasFoundSoFar() {
        SiteFetcher budgetOfOne = new SiteFetcher() {
            private boolean spent;

            @Override
            public FetchResult fetch(URI uri) {
                if (spent) {
                    throw new RequestBudgetExceededException(1);
                }
                spent = true;
                return new FetchResult(true, 200, Map.of(),
                        "<a href=\"/about\">About</a>");
            }
        };

        assertThat(discovery.discover(TARGET, budgetOfOne)).containsExactly(TARGET);
    }
}
