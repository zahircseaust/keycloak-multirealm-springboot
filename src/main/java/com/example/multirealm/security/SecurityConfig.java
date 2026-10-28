package com.example.multirealm.security;

import com.example.multirealm.config.KeycloakProperties;
import com.example.multirealm.config.TenantProperties;
import com.example.multirealm.constant.AppConstants;
import com.example.multirealm.service.TenantRegistry;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   MultiRealmAuthenticationManagerResolver realmResolver,
                                                   TenantResolver tenantResolver,
                                                   KeycloakProperties keycloak,
                                                   TenantRegistry tenantRegistry) throws Exception {

        // Reads "iss" from the token, then hands it to the validator for that realm
        JwtIssuerAuthenticationManagerResolver issuerResolver = new JwtIssuerAuthenticationManagerResolver(realmResolver);

        http
            .csrf(csrf -> csrf.disable()) // stateless API using bearer tokens
            .cors(cors -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/public/**").permitAll()
                .requestMatchers("/super-admin/**").hasRole("SUPER_ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.authenticationManagerResolver(issuerResolver))
            .addFilterAfter(new TenantMatchFilter(tenantResolver, keycloak, tenantRegistry),
                    BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(TenantProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(properties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", AppConstants.TENANT_HEADER));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
