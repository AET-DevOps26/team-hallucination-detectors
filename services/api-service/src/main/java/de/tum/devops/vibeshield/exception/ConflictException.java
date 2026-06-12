package de.tum.devops.vibeshield.exception;

import org.springframework.http.HttpStatus;

/** 409 — the request clashes with current state (duplicate website, scan already running). */
public class ConflictException extends ApiException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
