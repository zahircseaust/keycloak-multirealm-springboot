package com.example.multirealm.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Per-tenant settings the super admin sets when creating a tenant and can change later.
 * Every field is optional: null means "keep the current value" (or the default on create).
 */
public record TenantConfigRequest(
        @Size(max = 100) String displayName,

        /** Origin of the tenant's frontend, e.g. https://bti.app.example.com (no trailing path). */
        @Pattern(regexp = "^https?://[^\\s/]+(:\\d+)?$", message = "must be an origin like https://bti.app.example.com")
        String appUrl,

        // Login options
        Boolean registrationAllowed,
        Boolean resetPasswordAllowed,
        Boolean rememberMe,
        Boolean verifyEmail,
        Boolean loginWithEmailAllowed,
        Boolean bruteForceProtected,

        /** Keycloak password policy, e.g. "length(10) and digits(1) and upperCase(1)". */
        @Size(max = 500) String passwordPolicy,

        // Token and session lifetimes, in seconds
        @Min(60) @Max(86400) Integer accessTokenLifespan,
        @Min(300) @Max(604800) Integer ssoSessionIdleTimeout,
        @Min(300) @Max(2592000) Integer ssoSessionMaxLifespan,

        // Branding and language
        @Size(max = 50) String loginTheme,
        @Size(max = 50) String emailTheme,
        List<@Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$") String> supportedLocales,
        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$") String defaultLocale,

        @Valid SmtpConfigRequest smtp,

        /** Extra realm roles. tenant-admin and user always exist. Roles are only ever added. */
        List<@Pattern(regexp = "^[a-z][a-z0-9-]{1,40}$") String> roles,

        List<@Valid CreateClientScopeRequest> clientScopes,
        List<@Valid CreateClientRequest> clients,
        List<@Valid CreateUserRequest> users) {

    public static TenantConfigRequest empty() {
        return new TenantConfigRequest(null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
