package com.example.multirealm.repository;

import java.util.Collection;
import java.util.List;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Keycloak is the second data store: it holds the realms, clients, roles and users.
 * All Keycloak Admin API access goes through this repository.
 */
public interface KeycloakRealmRepository {

    boolean exists(String realm);

    RealmRepresentation find(String realm);

    void create(RealmRepresentation realm);

    void update(String realm, RealmRepresentation representation);

    void delete(String realm);

    /** Public React client with PKCE, redirecting to the given app origin. */
    void createWebClient(String realm, String appUrl);

    void updateWebClientUrl(String realm, String appUrl);

    List<ClientRepresentation> findClients(String realm);

    ClientRepresentation findClientByClientId(String realm, String clientId);

    String createClient(String realm, ClientRepresentation client);

    List<ClientScopeRepresentation> findClientScopes(String realm);

    String createClientScope(String realm, ClientScopeRepresentation scope);

    void assignClientScope(String realm, String clientId, String scopeId, boolean isDefault);

    /** Realm roles, without Keycloak's built-in ones. */
    List<String> findRoles(String realm);

    List<RoleRepresentation> findRoleRepresentations(String realm);

    void createRole(String realm, String roleName, String description);

    /** Adds roles that don't exist yet; existing roles are left alone. */
    void createMissingRoles(String realm, Collection<String> roles);

    /** Creates a user with a temporary password and gives them a realm role. */
    void createUserWithRole(String realm, String email, String tempPassword, String role);

    String createUser(String realm, String username, String email, String tempPassword, Collection<String> roles);

    List<UserRepresentation> findUsers(String realm);

    List<String> findUserRoles(String realm, String userId);
}
