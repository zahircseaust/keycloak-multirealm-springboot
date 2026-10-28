package com.example.multirealm.dto.response;

import java.util.List;

public record CurrentUserResponse(String tenant, String username, String email, List<String> roles) {
}
