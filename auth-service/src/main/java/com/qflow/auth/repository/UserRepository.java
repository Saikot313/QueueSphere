package com.qflow.auth.repository;

import com.qflow.auth.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByTenantIdAndUsername(Long tenantId, String username);

    boolean existsByTenantIdAndUsername(Long tenantId, String username);
}