package com.qflow.appointment.entity;

import com.qflow.common.model.Priority;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long tenantId;
    private Long queueId;
    private Long userId;
    private String citizenName;
    private String phone;
    private LocalDateTime slotTime;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    private Long tokenId;
    private Integer tokenNumber;
    private LocalDateTime createdAt;
}