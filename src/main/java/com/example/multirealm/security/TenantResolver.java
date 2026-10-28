package com.example.multirealm.security;

import com.example.multirealm.config.TenantProperties;
import com.example.multirealm.constant.AppConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Works out which tenant a request is for:
 *   1. the X-Tenant-ID header, if present
 *   2. otherwise the subdomain, e.g. bti.app.example.com -> "bti"
 */
@Component
public class TenantResolver {

    private final String baseDomain;

    public TenantResolver(TenantProperties properties) {
        this.baseDomain = properties.baseDomain();
    }

    public String resolve(HttpServletRequest request) {
        String header = request.getHeader(AppConstants.TENANT_HEADER);
        if (StringUtils.hasText(header)) {
            return header.trim().toLowerCase();
        }
        String host = request.getServerName();
        if (StringUtils.hasText(baseDomain) && host != null && host.endsWith("." + baseDomain)) {
            String sub = host.substring(0, host.length() - baseDomain.length() - 1);
            if (!sub.isEmpty() && !sub.contains(".")) {
                return sub.toLowerCase();
            }
        }
        return null;
    }
}
