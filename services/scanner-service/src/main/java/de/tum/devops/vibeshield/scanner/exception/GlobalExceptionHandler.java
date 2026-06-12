package de.tum.devops.vibeshield.scanner.exception;

import de.tum.devops.vibeshield.scanner.generated.model.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into the unified {@code { code, message, details }} error
 * shape shared by all VibeShield services, using the contract-generated Error model.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Error> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Error().code("VALIDATION_ERROR")
                        .message("Request is missing, malformed, or violates the contract."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
                .body(new Error().code("INTERNAL_ERROR").message("An unexpected error occurred."));
    }
}
