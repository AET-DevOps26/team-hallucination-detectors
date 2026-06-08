package de.tum.devops.vibeshield.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Unified error body returned by every failing request across VibeShield services.
 * Shape: {@code { code, message, details }}, kept identical in every service so clients
 * can parse one error contract.
 */
@Schema(description = "Unified error body shared across all VibeShield services.")
public record ErrorResponse(
        @Schema(description = "Machine-readable error code.", example = "INTERNAL_ERROR")
        String code,
        @Schema(description = "Human-readable error message.", example = "An unexpected error occurred.")
        String message,
        @Schema(description = "Optional structured details, e.g. field-level validation context.", nullable = true)
        Object details
) {
    /** Convenience for errors that carry no structured details. */
    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
