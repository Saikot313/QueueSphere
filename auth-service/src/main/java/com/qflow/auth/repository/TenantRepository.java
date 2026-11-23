package com.qflow.auth.repository;

import com.qflow.auth.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByCode(String code);

    boolean existsByCode(String code);
}