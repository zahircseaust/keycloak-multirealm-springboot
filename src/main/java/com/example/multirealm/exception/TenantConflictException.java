package com.example.multirealm.exception;

/** Tenant code already used or reserved, or a user already exists. */
public class TenantConflictException extends RuntimeException {
    public TenantConflictException(String message) {
        super(message);
    }
}
