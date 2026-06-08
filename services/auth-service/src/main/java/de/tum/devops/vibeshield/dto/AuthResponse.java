package de.tum.devops.vibeshield.dto;

public record AuthResponse(
        String token,
        String email
) {}
