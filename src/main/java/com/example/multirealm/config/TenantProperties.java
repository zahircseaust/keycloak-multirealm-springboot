package com.example.multirealm.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tenants")
public record TenantProperties(
        String superAdminRealm,
        String baseDomain,
        List<String> reservedCodes,
        List<String> allowedOrigins) {

    public boolean isReserved(String code) {
        return code.equals(superAdminRealm) || (reservedCodes != null && reservedCodes.contains(code));
    }
}
