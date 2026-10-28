package com.example.multirealm.security;

import com.example.multirealm.config.KeycloakProperties;
import com.example.multirealm.service.TenantRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.stereotype.Component;

/**
 * Given the token's issuer, returns a validator for that realm only: signature against the
 * realm's own keys, plus expiry and an exact issuer match. Untrusted issuers get null (rejected).
 * One validator is built per realm and cached.
 */
@Component
public class MultiRealmAuthenticationManagerResolver implements AuthenticationManagerResolver<String> {

    private final KeycloakProperties keycloak;
    private final TenantRegistry tenantRegistry;
    private final Map<String, AuthenticationManager> managers = new ConcurrentHashMap<>();

    public MultiRealmAuthenticationManagerResolver(KeycloakProperties keycloak, TenantRegistry tenantRegistry) {
        this.keycloak = keycloak;
        this.tenantRegistry = tenantRegistry;
    }

    @Override
    public AuthenticationManager resolve(String issuer) {
        String realm = keycloak.realmFromIssuer(issuer);
        if (!tenantRegistry.isTrusted(realm)) {
            return null;
        }
        return managers.computeIfAbsent(issuer, this::buildManager);
    }

    /** Drops the cached validator for a realm (after it is disabled or deleted). */
    public void evict(String realm) {
        managers.remove(keycloak.issuerFor(realm));
    }

    private AuthenticationManager buildManager(String issuer) {
        String realm = keycloak.realmFromIssuer(issuer);
        String jwkSetUri = keycloak.actualServerUrl() + "/realms/" + realm + "/protocol/openid-connect/certs";

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(new KeycloakRoleConverter());
        return provider::authenticate;
    }
}
