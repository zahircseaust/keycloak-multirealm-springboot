package com.example.multirealm.dto.response;

import java.util.List;

public record UserSummaryResponse(
        String id,
        String username,
        String email,
        Boolean enabled,
        List<String> roles
) {
}
