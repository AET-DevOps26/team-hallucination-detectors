package de.tum.devops.vibeshield.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Translates uncaught exceptions into a consistent JSON error response across the auth API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Logs the failure and returns a generic 500 body, hiding internal details from clients. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "error", "Internal Server Error",
                "message", "An unexpected error occurred."
        );
        return ResponseEntity.internalServerError().body(body);
    }
}
