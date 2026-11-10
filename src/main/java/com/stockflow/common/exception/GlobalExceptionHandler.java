package com.stockflow.common.exception;

import com.stockflow.common.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 - Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "Validation failed",
                errors
        );
        return ResponseEntity.status(status).body(response);
    }

    // 400 - Malformed JSON body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "Malformed request body"
        );
        return ResponseEntity.status(status).body(response);
    }

    // 400 - Type mismatch (e.g. invalid UUID format in path variable)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = String.format("Parameter '%s' has invalid value '%s'", ex.getName(), ex.getValue());
        ErrorResponse response = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message
        );
        return ResponseEntity.status(status).body(response);
    }

    // 401 - Unauthorized
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse response = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(status).body(response);
    }

    // 403 - Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ErrorResponse response = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Access denied"
        );
        return ResponseEntity.status(status).body(response);
    }

    // 404 - Not Found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse response = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(status).body(response);
    }

    // 409 / 400 - Illegal argument
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        boolean isConflict = ex.getMessage() != null
                && ex.getMessage().toLowerCase().contains("already exists");
        HttpStatus status = isConflict ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        ErrorResponse response = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(status).body(response);
    }

    // 409 - Business state conflict
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse response = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(status).body(response);
    }

    // 409 - Database constraint violation
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse response = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "Data integrity violation"
        );
        return ResponseEntity.status(status).body(response);
    }

    // 500 - General internal server error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse response = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "An unexpected error occurred"
        );
        return ResponseEntity.status(status).body(response);
    }
}
