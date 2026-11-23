package com.qflow.auth.controller;

import com.qflow.auth.dto.AuthDtos.*;
import com.qflow.auth.service.AuthService;
import com.qflow.common.constants.HeaderNames;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-tenant")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerTenant(@Valid @RequestBody RegisterTenantRequest req) {
        return authService.registerTenant(req);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerCitizen(@Valid @RequestBody RegisterCitizenRequest req) {
        return authService.registerCitizen(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_ROLE) String role,
            @Valid @RequestBody CreateUserRequest req) {
        return authService.createUser(tenantId, role, req);
    }
}