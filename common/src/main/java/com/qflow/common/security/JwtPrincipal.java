package com.qflow.common.security;

public record JwtPrincipal(Long userId, Long tenantId, String username, String role) {
}