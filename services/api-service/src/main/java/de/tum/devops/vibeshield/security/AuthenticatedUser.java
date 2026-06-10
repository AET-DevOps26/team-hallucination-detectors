package de.tum.devops.vibeshield.security;

/**
 * Identity of the caller, extracted from a validated JWT issued by the auth-service.
 * Controllers receive it via the {@link JwtAuthFilter#USER_ATTRIBUTE} request attribute,
 * e.g. {@code @RequestAttribute(JwtAuthFilter.USER_ATTRIBUTE) AuthenticatedUser user},
 * and use it for ownership checks on websites and scans.
 */
public record AuthenticatedUser(Long userId, String email) {
}
