package com.example.multirealm.security;

import com.example.multirealm.config.KeycloakProperties;
import com.example.multirealm.constant.AppConstants;
import com.example.multirealm.service.TenantRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs after the token is validated and decides which realm may call what:
 *  - /super-admin/**  only tokens from the super admin realm. A tenant cannot get in by creating
 *                     its own "super-admin" role, because the realm itself is checked.
 *  - everything else  the token's realm must equal the request's tenant (X-Tenant-ID or subdomain).
 */
public class TenantMatchFilter extends OncePerRequestFilter {

    private final TenantResolver tenantResolver;
    private final KeycloakProperties keycloak;
    private final TenantRegistry tenantRegistry;

    public TenantMatchFilter(TenantResolver tenantResolver, KeycloakProperties keycloak, TenantRegistry tenantRegistry) {
        this.tenantResolver = tenantResolver;
        this.keycloak = keycloak;
        this.tenantRegistry = tenantRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            chain.doFilter(request, response); // public endpoint
            return;
        }

        String tokenRealm = keycloak.realmFromIssuer(jwtAuth.getToken().getIssuer().toString());
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String tenant;

        if (path.startsWith(AppConstants.SUPER_ADMIN_PATH)) {
            if (!tenantRegistry.isSuperAdminRealm(tokenRealm)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Super admin access only");
                return;
            }
            tenant = tokenRealm;
        } else {
            tenant = tenantResolver.resolve(request);
            if (tenant == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant not specified");
                return;
            }
            if (!tenant.equals(tokenRealm)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token does not belong to this tenant");
                return;
            }
        }

        try {
            TenantContext.set(tenant);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
