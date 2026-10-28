package com.example.multirealm.controller;

import com.example.multirealm.dto.request.CreateRoleRequest;
import com.example.multirealm.dto.request.CreateUserRequest;
import com.example.multirealm.dto.response.CurrentUserResponse;
import com.example.multirealm.dto.response.RoleResponse;
import com.example.multirealm.dto.response.UserSummaryResponse;
import com.example.multirealm.security.TenantContext;
import com.example.multirealm.service.TenantService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints for tenant users and tenant admins. TenantMatchFilter guarantees the token belongs to the request's tenant. */
@RestController
@RequestMapping("/api")
public class TenantUserController {

    private final TenantService tenantService;

    public TenantUserController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(JwtAuthenticationToken auth) {
        return new CurrentUserResponse(
                TenantContext.get(),
                auth.getName(),
                auth.getToken().getClaimAsString("email"),
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    @GetMapping("/admin/summary")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public Map<String, String> adminSummary() {
        return Map.of("tenant", TenantContext.get(), "message", "Tenant admin area");
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<UserSummaryResponse> listUsers() {
        return tenantService.getUsers(TenantContext.get());
    }

    @PostMapping("/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UserSummaryResponse createUser(@Valid @RequestBody CreateUserRequest request, Authentication auth) {
        return tenantService.createUser(TenantContext.get(), request, auth.getName());
    }

    @GetMapping("/admin/roles")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<RoleResponse> listRoles() {
        return tenantService.getRoles(TenantContext.get());
    }

    @PostMapping("/admin/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public RoleResponse createRole(@Valid @RequestBody CreateRoleRequest request, Authentication auth) {
        return tenantService.createRole(TenantContext.get(), request, auth.getName());
    }
}
