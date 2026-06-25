package de.tum.devops.vibeshield.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.devops.vibeshield.dto.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Guards every {@code /api/**} endpoint with Bearer-JWT authentication. Requests with a
 * valid token continue with the caller's {@link AuthenticatedUser} exposed under the
 * {@link #USER_ATTRIBUTE} request attribute; all others are rejected with a 401 in the
 * unified {@code { code, message, details }} error shape.
 *
 * <p>Everything outside {@code /api/} (actuator, Swagger UI, OpenAPI docs) stays public,
 * as do the explicitly allow-listed API paths. New API endpoints are therefore protected
 * by default.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Request attribute under which the validated caller identity is exposed to controllers. */
    public static final String USER_ATTRIBUTE = "vibeshield.authenticatedUser";

    private static final String BEARER_PREFIX = "Bearer ";

    /** API paths that intentionally need no token (scaffold health endpoint used by the client). */
    private static final List<String> PUBLIC_API_PATHS = List.of("/api/v1/hello");

    /** Swagger UI and OpenAPI spec paths — public so the tutor can browse the docs without a token. */
    private static final List<String> SWAGGER_PATH_PREFIXES = List.of(
            "/api/swagger-ui", "/api/v3/api-docs", "/api/webjars"
    );

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        if (SWAGGER_PATH_PREFIXES.stream().anyMatch(path::startsWith)) {
            return true;
        }
        return PUBLIC_API_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "UNAUTHORIZED",
                    "Authentication required: send the token as 'Authorization: Bearer <token>'.");
            return;
        }

        try {
            AuthenticatedUser user = jwtService.authenticate(header.substring(BEARER_PREFIX.length()));
            request.setAttribute(USER_ATTRIBUTE, user);
        } catch (JwtException | IllegalArgumentException exception) {
            writeUnauthorized(response, "INVALID_TOKEN", "Token is invalid or expired.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Writes the unified error body; the filter runs before MVC, so the advice cannot do it. */
    private void writeUnauthorized(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(code, message));
    }
}
