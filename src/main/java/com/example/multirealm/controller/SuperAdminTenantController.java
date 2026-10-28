package com.example.multirealm.controller;

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
import com.example.multirealm.service.TenantService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Super admin console API. Only the super admin realm with role super-admin reaches here. */
@RestController
@RequestMapping("/super-admin/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminTenantController {

    private final TenantService tenantService;

    public SuperAdminTenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<TenantResponse> list() {
        return tenantService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request, Authentication auth) {
        return tenantService.create(request, auth.getName());
    }

    @GetMapping("/{code}")
    public TenantDetailsResponse get(@PathVariable String code) {
        return tenantService.findByCode(code);
    }

    /** Send only the settings to change, e.g. {"accessTokenLifespan": 600}. */
    @PatchMapping("/{code}/config")
    public TenantDetailsResponse updateConfig(@PathVariable String code,
                                              @Valid @RequestBody TenantConfigRequest config,
                                              Authentication auth) {
        return tenantService.updateConfig(code, config, auth.getName());
    }

    @PostMapping("/{code}/enable")
    public TenantResponse enable(@PathVariable String code, Authentication auth) {
        return tenantService.setEnabled(code, true, auth.getName());
    }

    @PostMapping("/{code}/disable")
    public TenantResponse disable(@PathVariable String code, Authentication auth) {
        return tenantService.setEnabled(code, false, auth.getName());
    }

    @PostMapping("/{code}/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public void addAdmin(@PathVariable String code, @Valid @RequestBody AddTenantAdminRequest request,
                         Authentication auth) {
        tenantService.addTenantAdmin(code, request, auth.getName());
    }

    // ---------- Scopes ----------

    @GetMapping("/{code}/client-scopes")
    public List<ClientScopeResponse> getClientScopes(@PathVariable String code) {
        return tenantService.getClientScopes(code);
    }

    @PostMapping("/{code}/client-scopes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientScopeResponse createClientScope(@PathVariable String code,
                                                 @Valid @RequestBody CreateClientScopeRequest request,
                                                 Authentication auth) {
        return tenantService.createClientScope(code, request, auth.getName());
    }

    // ---------- Clients ----------

    @GetMapping("/{code}/clients")
    public List<ClientSummaryResponse> getClients(@PathVariable String code) {
        return tenantService.getClients(code);
    }

    @PostMapping("/{code}/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientSummaryResponse createClient(@PathVariable String code,
                                              @Valid @RequestBody CreateClientRequest request,
                                              Authentication auth) {
        return tenantService.createClient(code, request, auth.getName());
    }

    // ---------- Roles ----------

    @GetMapping("/{code}/roles")
    public List<RoleResponse> getRoles(@PathVariable String code) {
        return tenantService.getRoles(code);
    }

    @PostMapping("/{code}/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse createRole(@PathVariable String code,
                                   @Valid @RequestBody CreateRoleRequest request,
                                   Authentication auth) {
        return tenantService.createRole(code, request, auth.getName());
    }

    // ---------- Users ----------

    @GetMapping("/{code}/users")
    public List<UserSummaryResponse> getUsers(@PathVariable String code) {
        return tenantService.getUsers(code);
    }

    @PostMapping("/{code}/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummaryResponse createUser(@PathVariable String code,
                                          @Valid @RequestBody CreateUserRequest request,
                                          Authentication auth) {
        return tenantService.createUser(code, request, auth.getName());
    }

    /** Permanent. Repeat the code to confirm: DELETE /super-admin/tenants/bti?confirm=bti */
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code, @RequestParam String confirm, Authentication auth) {
        if (!code.equals(confirm)) {
            return ResponseEntity.badRequest().build();
        }
        tenantService.delete(code, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
