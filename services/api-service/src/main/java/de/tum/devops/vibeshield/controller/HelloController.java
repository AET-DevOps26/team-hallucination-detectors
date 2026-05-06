package de.tum.devops.vibeshield.controller;

import de.tum.devops.vibeshield.dto.HelloResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HelloController {

    @Operation(
            summary = "Health greeting",
            description = "Returns a simple greeting payload to confirm the API is reachable."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Greeting returned successfully.",
            content = @Content(schema = @Schema(implementation = HelloResponse.class))
    )
    @GetMapping("/hello")
    public HelloResponse hello() {
        return new HelloResponse("Hello from VibeShield API");
    }
}
