package com.example.multirealm.repository.impl;

import com.example.multirealm.constant.AppConstants;
import com.example.multirealm.exception.TenantConflictException;
import com.example.multirealm.repository.KeycloakRealmRepository;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Repository;

@Repository
public class KeycloakRealmRepositoryImpl implements KeycloakRealmRepository {

    private static final Set<String> BUILT_IN_ROLES = Set.of("offline_access", "uma_authorization");

    private final Keycloak keycloak;

    public KeycloakRealmRepositoryImpl(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    // ---------- realm ----------

    @Override
    public boolean exists(String realm) {
        try {
            keycloak.realm(realm).toRepresentation();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public RealmRepresentation find(String realm) {
        return keycloak.realm(realm).toRepresentation();
    }

    @Override
    public void create(RealmRepresentation realm) {
        keycloak.realms().create(realm);
    }

    @Override
    public void update(String realm, RealmRepresentation representation) {
        keycloak.realm(realm).update(representation);
    }

    @Override
    public void delete(String realm) {
        keycloak.realm(realm).remove();
    }

    // ---------- client ----------

    @Override
    public void createWebClient(String realm, String appUrl) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(AppConstants.WEB_CLIENT_ID);
        client.setName("Web application");
        client.setPublicClient(true);               // React SPA: no secret
        client.setStandardFlowEnabled(true);        // authorization code flow
        client.setDirectAccessGrantsEnabled(false);
        client.setImplicitFlowEnabled(false);
        setUrls(client, appUrl);
        try (Response response = keycloak.realm(realm).clients().create(client)) {
            CreatedResponseUtil.getCreatedId(response); // throws if Keycloak refused
        }
    }

    @Override
    public void updateWebClientUrl(String realm, String appUrl) {
        ClientsResource clients = keycloak.realm(realm).clients();
        ClientRepresentation client = clients.findByClientId(AppConstants.WEB_CLIENT_ID).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Client web-app missing in realm " + realm));
        setUrls(client, appUrl);
        clients.get(client.getId()).update(client);
    }

    @Override
    public List<ClientRepresentation> findClients(String realm) {
        return keycloak.realm(realm).clients().findAll();
    }

    @Override
    public ClientRepresentation findClientByClientId(String realm, String clientId) {
        return keycloak.realm(realm).clients().findByClientId(clientId).stream().findFirst()
                .orElse(null);
    }

    @Override
    public String createClient(String realm, ClientRepresentation client) {
        try (Response response = keycloak.realm(realm).clients().create(client)) {
            if (response.getStatus() == 409) {
                throw new TenantConflictException("Client already exists in " + realm + ": " + client.getClientId());
            }
            return CreatedResponseUtil.getCreatedId(response);
        }
    }

    // ---------- client scopes ----------

    @Override
    public List<ClientScopeRepresentation> findClientScopes(String realm) {
        return keycloak.realm(realm).clientScopes().findAll();
    }

    @Override
    public String createClientScope(String realm, ClientScopeRepresentation scope) {
        try (Response response = keycloak.realm(realm).clientScopes().create(scope)) {
            if (response.getStatus() == 409) {
                throw new TenantConflictException("Client scope already exists in " + realm + ": " + scope.getName());
            }
            return CreatedResponseUtil.getCreatedId(response);
        }
    }

    @Override
    public void assignClientScope(String realm, String clientId, String scopeId, boolean isDefault) {
        ClientsResource clients = keycloak.realm(realm).clients();
        ClientRepresentation client = clients.findByClientId(clientId).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Client not found in " + realm + ": " + clientId));
        if (isDefault) {
            clients.get(client.getId()).addDefaultClientScope(scopeId);
        } else {
            clients.get(client.getId()).addOptionalClientScope(scopeId);
        }
    }

    private static void setUrls(ClientRepresentation client, String appUrl) {
        client.setRootUrl(appUrl);
        client.setRedirectUris(List.of(appUrl + "/*"));
        client.setWebOrigins(List.of(appUrl));
        Map<String, String> attrs = client.getAttributes() != null
                ? new HashMap<>(client.getAttributes()) : new HashMap<>();
        attrs.put("pkce.code.challenge.method", "S256");
        attrs.put("post.logout.redirect.uris", appUrl + "/*");
        client.setAttributes(attrs);
    }

    // ---------- roles ----------

    @Override
    public List<String> findRoles(String realm) {
        return keycloak.realm(realm).roles().list().stream()
                .map(RoleRepresentation::getName)
                .filter(name -> !BUILT_IN_ROLES.contains(name) && !name.startsWith("default-roles-"))
                .sorted()
                .toList();
    }

    @Override
    public List<RoleRepresentation> findRoleRepresentations(String realm) {
        return keycloak.realm(realm).roles().list().stream()
                .filter(r -> !BUILT_IN_ROLES.contains(r.getName()) && !r.getName().startsWith("default-roles-"))
                .toList();
    }

    @Override
    public void createRole(String realm, String roleName, String description) {
        try {
            keycloak.realm(realm).roles().create(new RoleRepresentation(roleName, description, false));
        } catch (jakarta.ws.rs.ClientErrorException e) {
            if (e.getResponse().getStatus() == 409) {
                throw new TenantConflictException("Role already exists in " + realm + ": " + roleName);
            }
            throw e;
        }
    }

    @Override
    public void createMissingRoles(String realm, Collection<String> roles) {
        RealmResource resource = keycloak.realm(realm);
        Set<String> existing = new HashSet<>(findRoles(realm));
        roles.stream()
                .filter(role -> !existing.contains(role))
                .forEach(role -> resource.roles().create(new RoleRepresentation(role, null, false)));
    }

    // ---------- users ----------

    @Override
    public void createUserWithRole(String realm, String email, String tempPassword, String role) {
        createUser(realm, email, email, tempPassword, List.of(role));
    }

    @Override
    public String createUser(String realm, String username, String email, String tempPassword, Collection<String> roles) {
        RealmResource resource = keycloak.realm(realm);

        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(tempPassword);
        password.setTemporary(true);                // must change at first login

        UserRepresentation user = new UserRepresentation();
        String effectiveUsername = username != null && !username.isBlank() ? username : email;
        user.setUsername(effectiveUsername);
        user.setEmail(email);
        user.setFirstName(effectiveUsername);
        user.setLastName("User");
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setCredentials(List.of(password));

        String userId;
        try (Response response = resource.users().create(user)) {
            if (response.getStatus() == 409) {
                throw new TenantConflictException("User already exists in " + realm + ": " + user.getUsername());
            }
            userId = CreatedResponseUtil.getCreatedId(response);
        }

        if (roles != null && !roles.isEmpty()) {
            List<RoleRepresentation> roleReps = roles.stream()
                    .map(roleName -> resource.roles().get(roleName).toRepresentation())
                    .toList();
            resource.users().get(userId).roles().realmLevel().add(roleReps);
        }
        return userId;
    }

    @Override
    public List<UserRepresentation> findUsers(String realm) {
        return keycloak.realm(realm).users().list();
    }

    @Override
    public List<String> findUserRoles(String realm, String userId) {
        return keycloak.realm(realm).users().get(userId).roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .filter(name -> !BUILT_IN_ROLES.contains(name) && !name.startsWith("default-roles-"))
                .toList();
    }
}
