package com.example.multirealm.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Super admin creates a tenant: realm + web client + roles + first tenant admin, with optional settings. */
public record CreateTenantRequest(
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,30}$",
                message = "lowercase letters, digits and dashes, 2-31 characters, starting with a letter")
        String code,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 8, max = 64) String tempPassword,
        @Valid TenantConfigRequest config) {
}
