package com.example.multirealm.service;

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
import java.util.List;

/** Everything the super admin can do to tenants. */
public interface TenantService {

    List<TenantResponse> findAll();

    TenantDetailsResponse findByCode(String code);

    TenantResponse create(CreateTenantRequest request, String superAdmin);

    TenantDetailsResponse updateConfig(String code, TenantConfigRequest config, String superAdmin);

    TenantResponse setEnabled(String code, boolean enabled, String superAdmin);

    void addTenantAdmin(String code, AddTenantAdminRequest request, String superAdmin);

    void delete(String code, String superAdmin);

    // Scopes
    List<ClientScopeResponse> getClientScopes(String code);

    ClientScopeResponse createClientScope(String code, CreateClientScopeRequest request, String superAdmin);

    // Clients
    List<ClientSummaryResponse> getClients(String code);

    ClientSummaryResponse createClient(String code, CreateClientRequest request, String superAdmin);

    // Roles
    List<RoleResponse> getRoles(String code);

    RoleResponse createRole(String code, CreateRoleRequest request, String superAdmin);

    // Users
    List<UserSummaryResponse> getUsers(String code);

    UserSummaryResponse createUser(String code, CreateUserRequest request, String superAdmin);
}
