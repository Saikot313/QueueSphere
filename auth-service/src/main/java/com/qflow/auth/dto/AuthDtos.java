package com.qflow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterTenantRequest(
            @NotBlank String tenantName,
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9-]+$") String tenantCode,
            @NotBlank String adminUsername,
            @NotBlank @Size(min = 6) String password,
            @NotBlank String fullName,
            String phone) {
    }

    public record RegisterCitizenRequest(
            @NotBlank String tenantCode,
            @NotBlank String username,
            @NotBlank @Size(min = 6) String password,
            @NotBlank String fullName,
            String phone) {
    }

    public record LoginRequest(
            @NotBlank String tenantCode,
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank @Size(min = 6) String password,
            @NotBlank String fullName,
            String phone,
            @NotBlank String role) {
    }

    public record AuthResponse(String token, String tokenType, Long userId, Long tenantId,
            String username, String role) {
    }

    public record UserResponse(Long id, String username, String fullName, String role) {
    }
}