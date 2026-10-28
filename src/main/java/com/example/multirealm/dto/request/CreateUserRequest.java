package com.example.multirealm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateUserRequest(
        @NotBlank @Email String email,
        String username,
        @NotBlank @Size(min = 8, max = 64) String tempPassword,
        List<String> roles
) {
}
