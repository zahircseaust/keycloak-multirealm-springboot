package com.example.multirealm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Super admin adds another admin to an existing tenant. */
public record AddTenantAdminRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String tempPassword) {
}
