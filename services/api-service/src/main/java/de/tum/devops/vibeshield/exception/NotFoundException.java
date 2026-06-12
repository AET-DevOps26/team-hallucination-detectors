package de.tum.devops.vibeshield.exception;

import org.springframework.http.HttpStatus;

/**
 * 404 — resource does not exist, or belongs to another user. The contract deliberately
 * answers both cases identically so resource IDs cannot be enumerated.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
