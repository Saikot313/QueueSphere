package com.qflow.queue.repository;

import com.qflow.queue.entity.ServiceQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceQueueRepository extends JpaRepository<ServiceQueue, Long> {
    Optional<ServiceQueue> findByIdAndTenantId(Long id, Long tenantId);

    List<ServiceQueue> findByTenantIdOrderByNameAsc(Long tenantId);

    boolean existsByTenantIdAndCode(Long tenantId, String code);
}