package com.example.multirealm.service.impl;

import com.example.multirealm.config.TenantProperties;
import com.example.multirealm.constant.AppConstants;
import com.example.multirealm.dto.request.AddTenantAdminRequest;
import com.example.multirealm.dto.request.CreateClientRequest;
import com.example.multirealm.dto.request.CreateClientScopeRequest;
import com.example.multirealm.dto.request.CreateRoleRequest;
import com.example.multirealm.dto.request.CreateTenantRequest;
import com.example.multirealm.dto.request.CreateUserRequest;
import com.example.multirealm.dto.request.TenantConfigRequest;
import com.example.multirealm.dto.response.ClientScopeResponse;
import com.example.multirealm.dto.response.ClientSummaryResponse;
import com.example.multirealm.dto.response.RoleResponse;
import com.example.multirealm.dto.response.TenantDetailsResponse;
import com.example.multirealm.dto.response.TenantResponse;
import com.example.multirealm.dto.response.UserSummaryResponse;
import com.example.multirealm.entity.Tenant;
import com.example.multirealm.entity.TenantStatus;
import com.example.multirealm.exception.TenantConflictException;
import com.example.multirealm.exception.TenantNotFoundException;
import com.example.multirealm.exception.TenantProvisioningException;
import com.example.multirealm.mapper.RealmConfigMapper;
import com.example.multirealm.mapper.TenantMapper;
import com.example.multirealm.repository.KeycloakRealmRepository;
import com.example.multirealm.repository.TenantRepository;
import com.example.multirealm.security.MultiRealmAuthenticationManagerResolver;
import com.example.multirealm.service.TenantRegistry;
import com.example.multirealm.service.TenantService;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Business rules for tenants. Keeps the tenants table and Keycloak in step,
 * and refreshes the trusted-realm list after every change.
 */
@Service
public class TenantServiceImpl implements TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantServiceImpl.class);

    private static final Set<String> BUILT_IN_CLIENTS = Set.of(
            "account", "account-console", "admin-cli", "broker", "realm-management", "security-admin-console"
    );

    private final TenantRepository tenantRepository;
    private final KeycloakRealmRepository realmRepository;
    private final TenantMapper tenantMapper;
    private final RealmConfigMapper realmConfigMapper;
    private final TenantRegistry tenantRegistry;
    private final MultiRealmAuthenticationManagerResolver tokenValidators;
    private final TenantProperties properties;

    public TenantServiceImpl(TenantRepository tenantRepository,
                             KeycloakRealmRepository realmRepository,
                             TenantMapper tenantMapper,
                             RealmConfigMapper realmConfigMapper,
                             TenantRegistry tenantRegistry,
                             MultiRealmAuthenticationManagerResolver tokenValidators,
                             TenantProperties properties) {
        this.tenantRepository = tenantRepository;
        this.realmRepository = realmRepository;
        this.tenantMapper = tenantMapper;
        this.realmConfigMapper = realmConfigMapper;
        this.tenantRegistry = tenantRegistry;
        this.tokenValidators = tokenValidators;
        this.properties = properties;
    }

    @Override
    public List<TenantResponse> findAll() {
        return tenantRepository.findAll().stream().map(tenantMapper::toResponse).toList();
    }

    @Override
    public TenantDetailsResponse findByCode(String code) {
        Tenant tenant = getTenant(code);
        RealmRepresentation realm = realmRepository.find(code);
        return new TenantDetailsResponse(
                tenantMapper.toResponse(tenant),
                realmConfigMapper.toResponse(realm, realmRepository.findRoles(code)));
    }

    /**
     * Realm -> web client -> custom scopes -> custom clients -> roles -> tenant admin -> extra users.
     * Rolls back everything if a step fails.
     */
    @Override
    public TenantResponse create(CreateTenantRequest request, String superAdmin) {
        String code = request.code();
        TenantConfigRequest config = request.config() != null ? request.config() : TenantConfigRequest.empty();

        if (properties.isReserved(code)) {
            throw new TenantConflictException("Tenant code is reserved: " + code);
        }
        if (config.appUrl() == null) {
            throw new IllegalArgumentException("config.appUrl is required when creating a tenant");
        }
        if (tenantRepository.existsByCode(code) || realmRepository.exists(code)) {
            throw new TenantConflictException("Tenant already exists: " + code);
        }

        String displayName = config.displayName() != null ? config.displayName() : code.toUpperCase();
        Tenant tenant = tenantRepository.save(
                new Tenant(code, displayName, config.appUrl(), TenantStatus.PROVISIONING, superAdmin));
        try {
            RealmRepresentation realm = realmConfigMapper.newRealm(code);
            realmConfigMapper.apply(realm, config);
            realmRepository.create(realm);
            realmRepository.createWebClient(code, config.appUrl());
            realmRepository.createMissingRoles(code, allRoles(config));
            realmRepository.createUserWithRole(code, request.adminEmail(), request.tempPassword(),
                    AppConstants.ROLE_TENANT_ADMIN);

            // Provision optional initial client scopes
            if (config.clientScopes() != null) {
                for (CreateClientScopeRequest scopeReq : config.clientScopes()) {
                    createClientScope(code, scopeReq, superAdmin);
                }
            }

            // Provision optional initial custom clients
            if (config.clients() != null) {
                for (CreateClientRequest clientReq : config.clients()) {
                    createClient(code, clientReq, superAdmin);
                }
            }

            // Provision optional extra users
            if (config.users() != null) {
                for (CreateUserRequest userReq : config.users()) {
                    createUser(code, userReq, superAdmin);
                }
            }

            tenant.setStatus(TenantStatus.ACTIVE);
            tenant = tenantRepository.save(tenant);
            tenantRegistry.refresh();
            log.info("Tenant {} created by {}", code, superAdmin);
            return tenantMapper.toResponse(tenant);
        } catch (RuntimeException e) {
            rollback(code, tenant);
            throw new TenantProvisioningException("Could not create tenant " + code + ": " + e.getMessage(), e);
        }
    }

    /** Changes only the settings that are sent. */
    @Override
    public TenantDetailsResponse updateConfig(String code, TenantConfigRequest config, String superAdmin) {
        Tenant tenant = getTenant(code);

        RealmRepresentation realm = realmRepository.find(code);
        realmConfigMapper.apply(realm, config);
        realmRepository.update(code, realm);

        if (config.appUrl() != null) {
            realmRepository.updateWebClientUrl(code, config.appUrl());
            tenant.setAppUrl(config.appUrl());
        }
        if (config.roles() != null) {
            realmRepository.createMissingRoles(code, config.roles());
        }
        if (config.displayName() != null) {
            tenant.setDisplayName(config.displayName());
        }
        if (config.clientScopes() != null) {
            for (CreateClientScopeRequest scopeReq : config.clientScopes()) {
                createClientScope(code, scopeReq, superAdmin);
            }
        }
        if (config.clients() != null) {
            for (CreateClientRequest clientReq : config.clients()) {
                createClient(code, clientReq, superAdmin);
            }
        }
        if (config.users() != null) {
            for (CreateUserRequest userReq : config.users()) {
                createUser(code, userReq, superAdmin);
            }
        }
        tenantRepository.save(tenant);
        log.info("Tenant {} configuration updated by {}", code, superAdmin);
        return findByCode(code);
    }

    /** Disabling blocks new logins in Keycloak and rejects existing tokens in this API immediately. */
    @Override
    public TenantResponse setEnabled(String code, boolean enabled, String superAdmin) {
        Tenant tenant = getTenant(code);

        RealmRepresentation realm = realmRepository.find(code);
        realm.setEnabled(enabled);
        realmRepository.update(code, realm);

        tenant.setStatus(enabled ? TenantStatus.ACTIVE : TenantStatus.DISABLED);
        tenant = tenantRepository.save(tenant);
        tenantRegistry.refresh();
        tokenValidators.evict(code);
        log.info("Tenant {} {} by {}", code, enabled ? "enabled" : "disabled", superAdmin);
        return tenantMapper.toResponse(tenant);
    }

    @Override
    public void addTenantAdmin(String code, AddTenantAdminRequest request, String superAdmin) {
        getTenant(code);
        realmRepository.createUserWithRole(code, request.email(), request.tempPassword(),
                AppConstants.ROLE_TENANT_ADMIN);
        log.info("Tenant admin {} added to {} by {}", request.email(), code, superAdmin);
    }

    /** Permanently deletes the realm and all its users. */
    @Override
    public void delete(String code, String superAdmin) {
        Tenant tenant = getTenant(code);
        if (realmRepository.exists(code)) {
            realmRepository.delete(code);
        }
        tenantRepository.delete(tenant);
        tenantRegistry.refresh();
        tokenValidators.evict(code);
        log.warn("Tenant {} deleted by {}", code, superAdmin);
    }

    // ---------- Scopes ----------

    @Override
    public List<ClientScopeResponse> getClientScopes(String code) {
        getTenant(code);
        return realmRepository.findClientScopes(code).stream()
                .map(s -> new ClientScopeResponse(s.getId(), s.getName(), s.getDescription(), s.getProtocol()))
                .toList();
    }

    @Override
    public ClientScopeResponse createClientScope(String code, CreateClientScopeRequest request, String superAdmin) {
        getTenant(code);
        ClientScopeRepresentation scope = new ClientScopeRepresentation();
        scope.setName(request.name());
        scope.setDescription(request.description());
        scope.setProtocol(request.protocol() != null ? request.protocol() : "openid-connect");

        String scopeId = realmRepository.createClientScope(code, scope);

        if (request.assignedToClients() != null && !request.assignedToClients().isEmpty()) {
            boolean isDefault = request.defaultScope() == null || request.defaultScope();
            for (String clientId : request.assignedToClients()) {
                realmRepository.assignClientScope(code, clientId, scopeId, isDefault);
            }
        }
        log.info("Client scope {} created for tenant {} by {}", request.name(), code, superAdmin);
        return new ClientScopeResponse(scopeId, scope.getName(), scope.getDescription(), scope.getProtocol());
    }

    // ---------- Clients ----------

    @Override
    public List<ClientSummaryResponse> getClients(String code) {
        getTenant(code);
        return realmRepository.findClients(code).stream()
                .filter(c -> !BUILT_IN_CLIENTS.contains(c.getClientId()))
                .map(c -> new ClientSummaryResponse(
                        c.getId(),
                        c.getClientId(),
                        c.getName(),
                        c.isPublicClient(),
                        c.getRedirectUris(),
                        c.getWebOrigins(),
                        c.getDefaultClientScopes(),
                        c.getOptionalClientScopes()
                ))
                .toList();
    }

    @Override
    public ClientSummaryResponse createClient(String code, CreateClientRequest request, String superAdmin) {
        getTenant(code);
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(request.clientId());
        client.setName(request.name() != null ? request.name() : request.clientId());
        client.setDescription(request.description());
        boolean isPublic = request.publicClient() == null || request.publicClient();
        client.setPublicClient(isPublic);
        if (!isPublic && request.secret() != null) {
            client.setSecret(request.secret());
        }
        client.setStandardFlowEnabled(request.standardFlowEnabled() == null || request.standardFlowEnabled());
        client.setDirectAccessGrantsEnabled(Boolean.TRUE.equals(request.directAccessGrantsEnabled()));
        client.setServiceAccountsEnabled(Boolean.TRUE.equals(request.serviceAccountsEnabled()));
        if (request.redirectUris() != null) {
            client.setRedirectUris(request.redirectUris());
        }
        if (request.webOrigins() != null) {
            client.setWebOrigins(request.webOrigins());
        }
        if (request.defaultClientScopes() != null) {
            client.setDefaultClientScopes(request.defaultClientScopes());
        }
        if (request.optionalClientScopes() != null) {
            client.setOptionalClientScopes(request.optionalClientScopes());
        }
        Map<String, String> attrs = new HashMap<>();
        attrs.put("pkce.code.challenge.method", "S256");
        client.setAttributes(attrs);

        String id = realmRepository.createClient(code, client);
        log.info("Client {} created for tenant {} by {}", request.clientId(), code, superAdmin);
        return new ClientSummaryResponse(
                id,
                client.getClientId(),
                client.getName(),
                client.isPublicClient(),
                client.getRedirectUris(),
                client.getWebOrigins(),
                client.getDefaultClientScopes(),
                client.getOptionalClientScopes()
        );
    }

    // ---------- Roles ----------

    @Override
    public List<RoleResponse> getRoles(String code) {
        getTenant(code);
        return realmRepository.findRoleRepresentations(code).stream()
                .map(r -> new RoleResponse(r.getId(), r.getName(), r.getDescription()))
                .toList();
    }

    @Override
    public RoleResponse createRole(String code, CreateRoleRequest request, String superAdmin) {
        getTenant(code);
        realmRepository.createRole(code, request.name(), request.description());
        log.info("Role {} created for tenant {} by {}", request.name(), code, superAdmin);
        return new RoleResponse(null, request.name(), request.description());
    }

    // ---------- Users ----------

    @Override
    public List<UserSummaryResponse> getUsers(String code) {
        getTenant(code);
        return realmRepository.findUsers(code).stream()
                .map(u -> new UserSummaryResponse(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.isEnabled(),
                        realmRepository.findUserRoles(code, u.getId())
                ))
                .toList();
    }

    @Override
    public UserSummaryResponse createUser(String code, CreateUserRequest request, String superAdmin) {
        getTenant(code);
        Collection<String> roles = request.roles() != null && !request.roles().isEmpty()
                ? request.roles()
                : List.of(AppConstants.ROLE_USER);

        realmRepository.createMissingRoles(code, roles);
        String username = request.username() != null && !request.username().isBlank() ? request.username() : request.email();
        String userId = realmRepository.createUser(code, username, request.email(), request.tempPassword(), roles);

        log.info("User {} created in tenant {} with roles {} by {}", username, code, roles, superAdmin);
        return new UserSummaryResponse(userId, username, request.email(), true, List.copyOf(roles));
    }

    // ---------- helpers ----------

    private Tenant getTenant(String code) {
        return tenantRepository.findByCode(code).orElseThrow(() -> new TenantNotFoundException(code));
    }

    private void rollback(String code, Tenant tenant) {
        try {
            if (realmRepository.exists(code)) {
                realmRepository.delete(code);
            }
        } catch (RuntimeException e) {
            log.error("Could not remove half-created realm {}; delete it in the Keycloak console", code, e);
        }
        tenantRepository.delete(tenant);
    }

    private static Set<String> allRoles(TenantConfigRequest config) {
        Set<String> roles = new LinkedHashSet<>(AppConstants.DEFAULT_TENANT_ROLES);
        if (config.roles() != null) {
            roles.addAll(config.roles());
        }
        return roles;
    }
}
