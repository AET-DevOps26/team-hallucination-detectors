package de.tum.devops.vibeshield.scanner.service;

import de.tum.devops.vibeshield.scanner.http.FetchResult;
import de.tum.devops.vibeshield.scanner.http.RequestBudgetExceededException;
import de.tum.devops.vibeshield.scanner.http.SiteFetcher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shallow (depth-1) same-origin crawl: follows links found in the homepage HTML
 * so a scan result can report the pages it actually looked at, instead of always
 * claiming a single-page site. Deliberately conservative: same-origin links
 * only (never third-party hosts), a small hard cap on how many links are
 * followed, and each candidate must resolve with a 200 before being reported as
 * a page — a dead link in the nav does not count as a page.
 *
 * <p>This only discovers and reports pages; it does not (yet) hand them to
 * other checks to scan. No check today produces page-specific findings — the
 * existing ones probe fixed absolute paths or site-wide transport/header
 * config, so re-running them per discovered page would just waste budget on
 * duplicate requests. Wiring a future page-content check (e.g. one that reads
 * each page's body) up to iterate over the discovered pages, instead of only
 * the start URL, is the natural next step once such a check exists.
 */
@Component
public class PageDiscovery {

    private static final int MAX_DISCOVERED_PAGES = 4;

    private static final Pattern ANCHOR_HREF =
            Pattern.compile("<a[^>]+href=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private static final List<String> IGNORED_SCHEMES =
            List.of("mailto:", "tel:", "javascript:", "data:");

    /**
     * Fetches the homepage, follows same-origin links found in it (one hop),
     * and returns every page confirmed reachable — always including the
     * target itself first. Never throws: a spent request budget or an
     * unreachable homepage just yields fewer (or zero) discovered pages.
     */
    public List<URI> discover(URI target, SiteFetcher fetcher) {
        List<URI> pages = new ArrayList<>();
        pages.add(target);

        FetchResult homepage;
        try {
            homepage = fetcher.fetch(target);
        } catch (RequestBudgetExceededException exhausted) {
            return pages;
        }
        if (!homepage.reachable()) {
            return pages;
        }

        for (URI candidate : sameOriginLinks(target, homepage.bodySnippet())) {
            if (pages.contains(candidate)) {
                continue;
            }
            try {
                FetchResult response = fetcher.fetch(candidate);
                if (response.reachable() && response.status() == 200) {
                    pages.add(candidate);
                }
            } catch (RequestBudgetExceededException exhausted) {
                break;
            }
        }

        return pages;
    }

    private List<URI> sameOriginLinks(URI target, String html) {
        List<URI> uris = new ArrayList<>();
        Matcher matcher = ANCHOR_HREF.matcher(html);
        while (matcher.find() && uris.size() < MAX_DISCOVERED_PAGES) {
            String href = matcher.group(1);
            if (href.startsWith("#") || isIgnoredScheme(href)) {
                continue;
            }
            URI resolved;
            try {
                resolved = target.resolve(href);
            } catch (IllegalArgumentException malformed) {
                continue;
            }
            if (isSameOrigin(target, resolved) && !uris.contains(resolved)) {
                uris.add(resolved);
            }
        }
        return uris;
    }

    private boolean isIgnoredScheme(String href) {
        String lower = href.toLowerCase(Locale.ROOT);
        return IGNORED_SCHEMES.stream().anyMatch(lower::startsWith);
    }

    private boolean isSameOrigin(URI target, URI candidate) {
        return target.getHost() != null && target.getHost().equalsIgnoreCase(candidate.getHost());
    }
}
