package com.example.multirealm.dto.response;

import com.example.multirealm.entity.TenantStatus;
import java.time.Instant;

public record TenantResponse(
        String code,
        String displayName,
        String appUrl,
        TenantStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {
}
