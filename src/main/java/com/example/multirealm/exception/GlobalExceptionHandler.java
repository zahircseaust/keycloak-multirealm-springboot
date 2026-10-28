package com.example.multirealm.exception;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns tenant and Keycloak errors into clear JSON (RFC 7807) responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({TenantNotFoundException.class, NotFoundException.class})
    public ProblemDetail notFound(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(TenantConflictException.class)
    public ProblemDetail conflict(TenantConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(TenantProvisioningException.class)
    public ProblemDetail provisioningFailed(TenantProvisioningException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(WebApplicationException.class)
    public ProblemDetail keycloakError(WebApplicationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "Keycloak rejected the request (" + e.getResponse().getStatus() + ")");
    }
}
