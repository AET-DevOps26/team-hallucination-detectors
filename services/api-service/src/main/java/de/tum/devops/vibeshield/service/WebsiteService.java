package de.tum.devops.vibeshield.service;

import de.tum.devops.vibeshield.exception.ConflictException;
import de.tum.devops.vibeshield.exception.ValidationException;
import de.tum.devops.vibeshield.model.Website;
import de.tum.devops.vibeshield.repository.WebsiteRepository;
import de.tum.devops.vibeshield.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Registration and listing of websites (issue #34). Enforces the contract rules:
 * absolute http(s) URLs only, display name defaults to the URL host, one
 * registration per URL per user.
 */
@Service
public class WebsiteService {

    private final WebsiteRepository websiteRepository;

    public WebsiteService(WebsiteRepository websiteRepository) {
        this.websiteRepository = websiteRepository;
    }

    @Transactional
    public Website create(AuthenticatedUser user, URI url, String name) {
        String normalizedUrl = validateUrl(url);
        if (websiteRepository.existsByOwnerIdAndUrl(user.userId(), normalizedUrl)) {
            throw new ConflictException("WEBSITE_ALREADY_REGISTERED",
                    "You already registered this URL.");
        }
        String displayName = (name == null || name.isBlank()) ? url.getHost() : name.trim();
        return websiteRepository.save(
                new Website(user.userId(), normalizedUrl, displayName, Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<Website> list(AuthenticatedUser user) {
        return websiteRepository.findAllByOwnerIdOrderByCreatedAtDescIdDesc(user.userId());
    }

    /** Accepts only absolute http(s) URLs with a host; returns the canonical string form. */
    private String validateUrl(URI url) {
        if (url == null || !url.isAbsolute() || url.getHost() == null || url.getHost().isBlank()) {
            throw new ValidationException("url must be an absolute http(s) URL.");
        }
        String scheme = url.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new ValidationException("url must use the http or https scheme.");
        }
        return url.toString();
    }
}
