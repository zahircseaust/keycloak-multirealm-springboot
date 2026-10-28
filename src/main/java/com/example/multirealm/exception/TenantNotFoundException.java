package com.example.multirealm.exception;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String code) {
        super("Tenant not found: " + code);
    }
}
