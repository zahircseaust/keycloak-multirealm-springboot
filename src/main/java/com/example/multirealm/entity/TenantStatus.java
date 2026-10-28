package com.example.multirealm.entity;

public enum TenantStatus {
    /** Realm is being created; tokens are not accepted yet. */
    PROVISIONING,
    /** Realm is live; users can log in and call the API. */
    ACTIVE,
    /** Disabled by the super admin; login and API access are blocked. */
    DISABLED
}
