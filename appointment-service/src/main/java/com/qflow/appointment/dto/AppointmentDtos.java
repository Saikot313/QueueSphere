package com.qflow.appointment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qflow.appointment.entity.AppointmentStatus;
import com.qflow.common.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class AppointmentDtos {
    private AppointmentDtos() {
    }

    public record BookRequest(@NotNull Long queueId, @NotNull LocalDateTime slotTime,
            @NotBlank String citizenName, String phone, Priority priority) {
    }

    public record AppointmentResponse(Long id, Long queueId, String citizenName, String phone,
            LocalDateTime slotTime, Priority priority, AppointmentStatus status,
            Long tokenId, Integer tokenNumber) {
    }

    public record SlotView(LocalDateTime slotTime, int capacity, int available) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenView(Long id, Integer tokenNumber) {
    }
}