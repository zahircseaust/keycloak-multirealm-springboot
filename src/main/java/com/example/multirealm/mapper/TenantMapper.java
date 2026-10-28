package com.example.multirealm.mapper;

import com.example.multirealm.dto.response.TenantResponse;
import com.example.multirealm.entity.Tenant;
import org.springframework.stereotype.Component;

/** Entity -> response DTO. */
@Component
public class TenantMapper {

    public TenantResponse toResponse(Tenant t) {
        return new TenantResponse(t.getCode(), t.getDisplayName(), t.getAppUrl(), t.getStatus(),
                t.getCreatedBy(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
