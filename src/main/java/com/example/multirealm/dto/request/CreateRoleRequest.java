package com.example.multirealm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateRoleRequest(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9-_:.]+$", message = "alphanumeric characters, dashes, colons, dots, and underscores only")
        String name,
        String description
) {
}
