package com.example.multirealm.dto.response;

import java.util.List;

public record ClientSummaryResponse(
        String id,
        String clientId,
        String name,
        Boolean publicClient,
        List<String> redirectUris,
        List<String> webOrigins,
        List<String> defaultClientScopes,
        List<String> optionalClientScopes
) {
}
