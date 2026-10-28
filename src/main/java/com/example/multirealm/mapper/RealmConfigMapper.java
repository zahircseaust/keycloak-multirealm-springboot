package com.example.multirealm.mapper;

import com.example.multirealm.dto.request.SmtpConfigRequest;
import com.example.multirealm.dto.request.TenantConfigRequest;
import com.example.multirealm.dto.response.TenantConfigResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.stereotype.Component;

/** Maps tenant settings to and from Keycloak's RealmRepresentation. */
@Component
public class RealmConfigMapper {

    /** Safe defaults for a brand-new tenant realm. */
    public RealmRepresentation newRealm(String code) {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm(code);
        rep.setDisplayName(code.toUpperCase());
        rep.setEnabled(true);
        rep.setLoginWithEmailAllowed(true);
        rep.setResetPasswordAllowed(true);
        rep.setRegistrationAllowed(false);
        rep.setBruteForceProtected(true);
        rep.setAccessTokenLifespan(300);          // 5 minutes
        rep.setSsoSessionIdleTimeout(1800);      // 30 minutes
        rep.setSsoSessionMaxLifespan(36000);     // 10 hours
        rep.setPasswordPolicy("length(8) and digits(1) and upperCase(1) and notUsername");
        return rep;
    }

    /** Copies only the non-null settings onto the realm. */
    public void apply(RealmRepresentation rep, TenantConfigRequest c) {
        if (c == null) {
            return;
        }
        if (c.displayName() != null) rep.setDisplayName(c.displayName());
        if (c.registrationAllowed() != null) rep.setRegistrationAllowed(c.registrationAllowed());
        if (c.resetPasswordAllowed() != null) rep.setResetPasswordAllowed(c.resetPasswordAllowed());
        if (c.rememberMe() != null) rep.setRememberMe(c.rememberMe());
        if (c.verifyEmail() != null) rep.setVerifyEmail(c.verifyEmail());
        if (c.loginWithEmailAllowed() != null) rep.setLoginWithEmailAllowed(c.loginWithEmailAllowed());
        if (c.bruteForceProtected() != null) rep.setBruteForceProtected(c.bruteForceProtected());
        if (c.passwordPolicy() != null) rep.setPasswordPolicy(c.passwordPolicy());
        if (c.accessTokenLifespan() != null) rep.setAccessTokenLifespan(c.accessTokenLifespan());
        if (c.ssoSessionIdleTimeout() != null) rep.setSsoSessionIdleTimeout(c.ssoSessionIdleTimeout());
        if (c.ssoSessionMaxLifespan() != null) rep.setSsoSessionMaxLifespan(c.ssoSessionMaxLifespan());
        if (c.loginTheme() != null) rep.setLoginTheme(c.loginTheme());
        if (c.emailTheme() != null) rep.setEmailTheme(c.emailTheme());
        if (c.supportedLocales() != null && !c.supportedLocales().isEmpty()) {
            rep.setInternationalizationEnabled(true);
            rep.setSupportedLocales(new HashSet<>(c.supportedLocales()));
        }
        if (c.defaultLocale() != null) rep.setDefaultLocale(c.defaultLocale());
        if (c.smtp() != null) rep.setSmtpServer(mergeSmtp(rep.getSmtpServer(), c.smtp()));
    }

    public TenantConfigResponse toResponse(RealmRepresentation r, List<String> roles) {
        Map<String, String> smtp = r.getSmtpServer() != null ? r.getSmtpServer() : Map.of();
        return new TenantConfigResponse(
                r.getDisplayName(), Boolean.TRUE.equals(r.isEnabled()),
                r.isRegistrationAllowed(), r.isResetPasswordAllowed(), r.isRememberMe(), r.isVerifyEmail(),
                r.isLoginWithEmailAllowed(), r.isBruteForceProtected(), r.getPasswordPolicy(),
                r.getAccessTokenLifespan(), r.getSsoSessionIdleTimeout(), r.getSsoSessionMaxLifespan(),
                r.getLoginTheme(), r.getEmailTheme(), r.getSupportedLocales(), r.getDefaultLocale(),
                smtp.get("host"), smtp.get("port"), smtp.get("from"),
                roles);
    }

    private static Map<String, String> mergeSmtp(Map<String, String> current, SmtpConfigRequest s) {
        Map<String, String> smtp = current != null ? new HashMap<>(current) : new HashMap<>();
        putIfSet(smtp, "host", s.host());
        putIfSet(smtp, "port", s.port() != null ? s.port().toString() : null);
        putIfSet(smtp, "from", s.from());
        putIfSet(smtp, "fromDisplayName", s.fromDisplayName());
        putIfSet(smtp, "ssl", s.ssl() != null ? s.ssl().toString() : null);
        putIfSet(smtp, "starttls", s.starttls() != null ? s.starttls().toString() : null);
        if (s.user() != null) {
            smtp.put("auth", "true");
            smtp.put("user", s.user());
        }
        putIfSet(smtp, "password", s.password()); // left out = Keycloak keeps the stored password
        return smtp;
    }

    private static void putIfSet(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
