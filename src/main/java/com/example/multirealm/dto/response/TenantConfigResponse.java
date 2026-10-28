package com.example.multirealm.dto.response;

import java.util.List;
import java.util.Set;

/** Current realm settings as read back from Keycloak. The SMTP password is never returned. */
public record TenantConfigResponse(
        String displayName,
        boolean enabled,
        Boolean registrationAllowed,
        Boolean resetPasswordAllowed,
        Boolean rememberMe,
        Boolean verifyEmail,
        Boolean loginWithEmailAllowed,
        Boolean bruteForceProtected,
        String passwordPolicy,
        Integer accessTokenLifespan,
        Integer ssoSessionIdleTimeout,
        Integer ssoSessionMaxLifespan,
        String loginTheme,
        String emailTheme,
        Set<String> supportedLocales,
        String defaultLocale,
        String smtpHost,
        String smtpPort,
        String smtpFrom,
        List<String> roles) {
}
