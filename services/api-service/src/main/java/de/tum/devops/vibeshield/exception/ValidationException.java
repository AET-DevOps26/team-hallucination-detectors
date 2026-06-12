package de.tum.devops.vibeshield.exception;

import org.springframework.http.HttpStatus;

/** 400 — semantically invalid input that bean validation cannot express (e.g. non-http URL). */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
