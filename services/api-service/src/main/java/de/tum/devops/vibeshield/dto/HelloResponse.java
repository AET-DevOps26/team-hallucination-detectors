package de.tum.devops.vibeshield.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Greeting payload returned by the hello endpoint.")
public record HelloResponse(
        @Schema(description = "Human-readable greeting.", example = "Hello from VibeShield API")
        String message
) {
}
