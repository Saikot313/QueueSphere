package com.qflow.notification.repository;

import com.qflow.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByTenantIdOrderByIdDesc(Long tenantId);
}