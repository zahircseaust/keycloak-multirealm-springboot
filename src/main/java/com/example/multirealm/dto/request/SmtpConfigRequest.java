package com.example.multirealm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Outgoing email for the tenant's realm (password reset, email verification). Null fields stay unchanged. */
public record SmtpConfigRequest(
        String host,
        @Min(1) @Max(65535) Integer port,
        @Email String from,
        String fromDisplayName,
        String user,
        String password,
        Boolean ssl,
        Boolean starttls) {
}
