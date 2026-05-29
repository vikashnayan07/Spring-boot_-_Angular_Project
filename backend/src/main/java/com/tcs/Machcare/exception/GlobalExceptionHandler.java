package com.tcs.Machcare.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tcs.Machcare.dto.CsvUploadResponse;
import com.tcs.Machcare.dto.ErrorResponse;
import com.tcs.Machcare.exception.CustomException.InsufficientStockException;
import com.tcs.Machcare.exception.CustomException.ResourceNotFoundException;
import com.tcs.Machcare.exception.CustomException.UnauthorizedAccessException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================
    // 1. ALERTS & INVENTORY EXCEPTIONS (NEW)
    // ==========================================

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedAccessException ex) {
        // Returns 403 Forbidden (e.g., trying to edit someone else's task)
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleStock(InsufficientStockException ex) {
        // Returns 400 Bad Request (e.g., Requesting 5 motors when only 2 are left)
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        // Returns 404 Not Found (e.g., Task ID or Part ID doesn't exist)
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    // ==========================================
    // 2. AUTHENTICATION & EMPLOYEE EXCEPTIONS
    // ==========================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        // Returns 400 Bad Request (Used in AuthService for "Account is disabled until...")
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CsvValidationException.class)
    public ResponseEntity<CsvUploadResponse> handleCsvValidation(CsvValidationException ex) {
        return ResponseEntity.badRequest().body(
                CsvUploadResponse.invalid(ex.getMessage(), ex.getMissingHeaders(), ex.getErrors())
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return new ResponseEntity<>(
                new ErrorResponse(status.value(), status.getReasonPhrase(), ex.getReason()),
                status
        );
    }

    // ==========================================
    // 3. SPRING & DATABASE EXCEPTIONS (ORIGINAL)
    // ==========================================

    // Handles Duplicate Emails / Database Constraints (Returns 409)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseExceptions(DataIntegrityViolationException ex) {
        String message = "Database error: A constraint was violated.";
        // Added a null check to prevent NullPointerExceptions when reading the root cause
        if (ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage().contains("duplicate key value")) {
            message = "An account with this email or username already exists.";
        }
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.CONFLICT.value(), "Conflict", message), HttpStatus.CONFLICT);
    }

    // Handles Bad Enums & Missing JSON Bodies (Returns 400)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Malformed JSON request, missing body, or invalid data types."), HttpStatus.BAD_REQUEST);
    }

    // Handles Wrong HTTP Methods, like sending a GET instead of a POST (Returns 405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.METHOD_NOT_ALLOWED.value(), "Method Not Allowed", ex.getMessage()), HttpStatus.METHOD_NOT_ALLOWED);
    }

    // Catch-all for anything else (Returns 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", "Something went wrong on the server: " + ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
