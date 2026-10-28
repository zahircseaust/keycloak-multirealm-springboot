package com.example.multirealm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CreateClientRequest(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "alphanumeric characters, dashes, and underscores only")
        String clientId,
        String name,
        String description,
        Boolean publicClient,
        String secret,
        Boolean standardFlowEnabled,
        Boolean directAccessGrantsEnabled,
        Boolean serviceAccountsEnabled,
        List<String> redirectUris,
        List<String> webOrigins,
        List<String> defaultClientScopes,
        List<String> optionalClientScopes
) {
}
