package com.gokarting.adapter.in.web;

import com.gokarting.domain.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Translates domain exceptions to RFC 7807 Problem Details responses.
 * Every error response has the same shape: type, title, status, detail, traceId.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String BASE_URI = "https://gokarting.api/problems/";

    @ExceptionHandler(SlotFullException.class)
    public ProblemDetail handleSlotFull(SlotFullException ex) {
        return problem(HttpStatus.CONFLICT, "slot-capacity-exceeded", "Time Slot Is Full", ex.getMessage());
    }

    @ExceptionHandler(DuplicateBookingException.class)
    public ProblemDetail handleDuplicate(DuplicateBookingException ex) {
        return problem(HttpStatus.CONFLICT, "duplicate-booking", "Booking Already Exists", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "resource-not-found", "Resource Not Found", ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserExists(UserAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, "user-already-exists", "Username Taken", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        // Intentionally vague to prevent user enumeration
        return problem(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Authentication Failed",
                "Invalid username or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "access-denied", "Access Denied", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return problem(HttpStatus.BAD_REQUEST, "validation-error", "Invalid Request", detail);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Internal Server Error", "An unexpected error occurred");
    }

    private ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(BASE_URI + type));
        pd.setTitle(title);
        pd.setProperty("traceId", MDC.get("traceId"));
        return pd;
    }
}
