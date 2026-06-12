package de.tum.devops.vibeshield.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for errors that map directly to a unified {@code { code, message, details }}
 * response with a specific HTTP status. Thrown from services, translated centrally by
 * the {@link GlobalExceptionHandler}.
 */
public abstract class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
