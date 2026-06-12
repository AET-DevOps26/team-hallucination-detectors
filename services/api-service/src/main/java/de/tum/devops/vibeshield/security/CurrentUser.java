package de.tum.devops.vibeshield.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Gives controllers/services access to the caller identity that {@link JwtAuthFilter}
 * attached to the current request. Implemented as a bean (backed by Spring's
 * request-scoped proxy) so generated API interfaces, which cannot declare extra
 * parameters, still get the identity without touching the servlet API themselves.
 */
@Component
public class CurrentUser {

    private final HttpServletRequest request;

    public CurrentUser(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * The authenticated caller of the current request. The filter guarantees presence on
     * every protected route; absence means a route was wrongly left off the filter.
     */
    public AuthenticatedUser require() {
        Object user = request.getAttribute(JwtAuthFilter.USER_ATTRIBUTE);
        if (user == null) {
            throw new IllegalStateException(
                    "No authenticated user on request — endpoint is not covered by JwtAuthFilter");
        }
        return (AuthenticatedUser) user;
    }
}
