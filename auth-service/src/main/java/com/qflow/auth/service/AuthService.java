package com.qflow.auth.service;

import com.qflow.auth.dto.AuthDtos.*;
import com.qflow.auth.entity.AppUser;
import com.qflow.auth.entity.Tenant;
import com.qflow.auth.repository.TenantRepository;
import com.qflow.auth.repository.UserRepository;
import com.qflow.common.exception.ApiException;
import com.qflow.common.security.JwtService;
import com.qflow.common.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Set<String> VALID_ROLES = Set.of(Roles.ADMIN, Roles.STAFF, Roles.CITIZEN);

    private final TenantRepository tenantRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse registerTenant(RegisterTenantRequest r) {
        String code = r.tenantCode().toLowerCase();
        if (tenantRepo.existsByCode(code)) {
            throw ApiException.conflict("Tenant code already taken");
        }
        Tenant tenant = new Tenant();
        tenant.setName(r.tenantName());
        tenant.setCode(code);
        tenant = tenantRepo.save(tenant);

        AppUser admin = buildUser(tenant.getId(), r.adminUsername(), r.password(), r.fullName(), r.phone(),
                Roles.ADMIN);
        return toResponse(userRepo.save(admin));
    }

    @Transactional
    public AuthResponse registerCitizen(RegisterCitizenRequest r) {
        Tenant tenant = tenantRepo.findByCode(r.tenantCode().toLowerCase())
                .orElseThrow(() -> ApiException.notFound("Tenant not found"));
        if (userRepo.existsByTenantIdAndUsername(tenant.getId(), r.username())) {
            throw ApiException.conflict("Username already exists");
        }
        AppUser u = buildUser(tenant.getId(), r.username(), r.password(), r.fullName(), r.phone(), Roles.CITIZEN);
        return toResponse(userRepo.save(u));
    }

    @Transactional
    public UserResponse createUser(Long tenantId, String callerRole, CreateUserRequest r) {
        Roles.require(callerRole, Roles.ADMIN);
        if (!VALID_ROLES.contains(r.role())) {
            throw ApiException.badRequest("Role must be one of " + VALID_ROLES);
        }
        if (userRepo.existsByTenantIdAndUsername(tenantId, r.username())) {
            throw ApiException.conflict("Username already exists");
        }
        AppUser u = userRepo.save(buildUser(tenantId, r.username(), r.password(), r.fullName(), r.phone(), r.role()));
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getRole());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        Tenant tenant = tenantRepo.findByCode(r.tenantCode().toLowerCase())
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        AppUser u = userRepo.findByTenantIdAndUsername(tenant.getId(), r.username())
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        if (!encoder.matches(r.password(), u.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }
        return toResponse(u);
    }

    private AppUser buildUser(Long tenantId, String username, String password, String fullName, String phone,
            String role) {
        AppUser u = new AppUser();
        u.setTenantId(tenantId);
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        u.setFullName(fullName);
        u.setPhone(phone);
        u.setRole(role);
        return u;
    }

    private AuthResponse toResponse(AppUser u) {
        String token = jwtService.generate(u.getId(), u.getTenantId(), u.getUsername(), u.getRole());
        return new AuthResponse(token, "Bearer", u.getId(), u.getTenantId(), u.getUsername(), u.getRole());
    }
}