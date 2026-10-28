package com.example.multirealm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CreateClientScopeRequest(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9-_:.]+$", message = "letters, digits, dashes, colons, dots, and underscores only")
        String name,
        String description,
        String protocol,
        List<String> assignedToClients,
        Boolean defaultScope
) {
}
