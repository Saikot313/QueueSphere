package com.qflow.appointment.repository;

import com.qflow.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findByIdAndTenantId(Long id, Long tenantId);

    List<Appointment> findByTenantIdAndUserIdOrderBySlotTimeDesc(Long tenantId, Long userId);

    List<Appointment> findByTenantIdAndQueueIdOrderBySlotTimeAsc(Long tenantId, Long queueId);
}