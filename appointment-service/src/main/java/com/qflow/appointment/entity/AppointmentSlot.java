package com.qflow.appointment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_slots")
@Getter
@Setter
@NoArgsConstructor
public class AppointmentSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long tenantId;
    private Long queueId;
    private LocalDateTime slotTime;
    private int capacity;
    private int booked;
}