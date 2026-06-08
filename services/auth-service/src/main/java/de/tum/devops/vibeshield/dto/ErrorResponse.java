package de.tum.devops.vibeshield.dto;

/**
 * Unified error body returned by every failing request across VibeShield services.
 * Shape: {@code { code, message, details }} — a machine-readable {@code code}, a
 * human-readable {@code message}, and optional {@code details} (e.g. field-level
 * validation context), which is {@code null} when there is nothing structured to add.
 */
public record ErrorResponse(
        String code,
        String message,
        Object details
) {
    /** Convenience for errors that carry no structured details. */
    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
