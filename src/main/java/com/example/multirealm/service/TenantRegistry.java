package com.example.multirealm.service;

/**
 * Realms this API trusts: the super admin realm plus every ACTIVE tenant.
 * A token from any other realm is rejected before its signature is checked.
 */
public interface TenantRegistry {

    boolean isTrusted(String realm);

    boolean isSuperAdminRealm(String realm);

    /** Reloads the trusted list after a tenant is created, enabled, disabled or deleted. */
    void refresh();
}
