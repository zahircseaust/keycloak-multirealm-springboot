package com.example.multirealm.constant;

import java.util.List;

public final class AppConstants {

    private AppConstants() {
    }

    // Keycloak realm roles
    public static final String ROLE_SUPER_ADMIN = "super-admin";
    public static final String ROLE_TENANT_ADMIN = "tenant-admin";
    public static final String ROLE_USER = "user";
    public static final List<String> DEFAULT_TENANT_ROLES = List.of(ROLE_TENANT_ADMIN, ROLE_USER);

    // Keycloak client created in every tenant realm for the React app
    public static final String WEB_CLIENT_ID = "web-app";

    // HTTP
    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String SUPER_ADMIN_PATH = "/super-admin/";
}
