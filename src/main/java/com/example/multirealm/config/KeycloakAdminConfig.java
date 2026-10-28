package com.example.multirealm.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One shared, thread-safe Keycloak Admin API client. It refreshes its own token. */
@Configuration
public class KeycloakAdminConfig {

    @Bean(destroyMethod = "close")
    public Keycloak keycloakAdmin(KeycloakProperties props) {
        return KeycloakBuilder.builder()
                .serverUrl(props.actualServerUrl())
                .realm(props.adminRealm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(props.adminClientId())
                .clientSecret(props.adminClientSecret())
                .build();
    }
}
