package com.example.multirealm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.keycloak")
public record KeycloakProperties(
        String baseUrl,
        String serverUrl,
        String adminRealm,
        String adminClientId,
        String adminClientSecret) {

    /** Internal server URL for direct HTTP calls from the backend (defaults to baseUrl). */
    public String actualServerUrl() {
        return (serverUrl != null && !serverUrl.isBlank()) ? serverUrl : baseUrl;
    }

    /** Issuer of a realm, exactly as Keycloak writes it in the "iss" claim. */
    public String issuerFor(String realm) {
        return baseUrl + "/realms/" + realm;
    }

    /** Returns the realm name if the issuer belongs to this Keycloak, otherwise null. */
    public String realmFromIssuer(String issuer) {
        String prefix = baseUrl + "/realms/";
        if (issuer == null || !issuer.startsWith(prefix)) {
            return null;
        }
        String realm = issuer.substring(prefix.length());
        return realm.isBlank() || realm.contains("/") ? null : realm;
    }
}
