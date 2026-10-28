package com.example.multirealm.exception;

/** Creating a tenant failed in Keycloak; everything created so far was rolled back. */
public class TenantProvisioningException extends RuntimeException {
    public TenantProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
