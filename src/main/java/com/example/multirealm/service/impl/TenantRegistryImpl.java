package com.example.multirealm.service.impl;

import com.example.multirealm.config.TenantProperties;
import com.example.multirealm.entity.Tenant;
import com.example.multirealm.entity.TenantStatus;
import com.example.multirealm.repository.TenantRepository;
import com.example.multirealm.service.TenantRegistry;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Keeps the trusted realms in memory for speed; reloaded from the database on every tenant change. */
@Service
public class TenantRegistryImpl implements TenantRegistry {

    private final TenantRepository tenantRepository;
    private final String superAdminRealm;
    private volatile Set<String> activeTenants = Set.of();

    public TenantRegistryImpl(TenantRepository tenantRepository, TenantProperties properties) {
        this.tenantRepository = tenantRepository;
        this.superAdminRealm = properties.superAdminRealm();
    }

    @PostConstruct
    @Override
    public void refresh() {
        activeTenants = tenantRepository.findByStatus(TenantStatus.ACTIVE).stream()
                .map(Tenant::getCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isTrusted(String realm) {
        return realm != null && (realm.equals(superAdminRealm) || activeTenants.contains(realm));
    }

    @Override
    public boolean isSuperAdminRealm(String realm) {
        return superAdminRealm.equals(realm);
    }
}
