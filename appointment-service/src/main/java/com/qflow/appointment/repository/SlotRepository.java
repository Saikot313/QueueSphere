package com.qflow.appointment.repository;

import com.qflow.appointment.entity.AppointmentSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<AppointmentSlot, Long> {

    @Modifying
    @Query(value = "INSERT INTO appointment_slots (tenant_id, queue_id, slot_time, capacity, booked) " +
            "VALUES (:tenantId, :queueId, :slotTime, :capacity, 0) " +
            "ON CONFLICT (tenant_id, queue_id, slot_time) DO NOTHING", nativeQuery = true)
    void insertIfAbsent(@Param("tenantId") Long tenantId, @Param("queueId") Long queueId,
            @Param("slotTime") LocalDateTime slotTime, @Param("capacity") int capacity);

    /**
     * Row lock (SELECT ... FOR UPDATE): concurrent bookings of the same slot are
     * serialized.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AppointmentSlot s where s.tenantId = :tenantId and s.queueId = :queueId and s.slotTime = :slotTime")
    Optional<AppointmentSlot> findForUpdate(@Param("tenantId") Long tenantId, @Param("queueId") Long queueId,
            @Param("slotTime") LocalDateTime slotTime);

    List<AppointmentSlot> findByTenantIdAndQueueIdAndSlotTimeBetween(Long tenantId, Long queueId,
            LocalDateTime from, LocalDateTime to);
}