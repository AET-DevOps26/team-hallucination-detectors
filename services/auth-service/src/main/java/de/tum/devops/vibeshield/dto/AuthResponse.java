package de.tum.devops.vibeshield.dto;

/**
 * Response body for a successful login: the signed JWT and the authenticated user's email.
 */
public record AuthResponse(
        String token,
        String email
) {}
