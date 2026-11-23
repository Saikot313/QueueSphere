package com.qflow.appointment.controller;

import com.qflow.appointment.dto.AppointmentDtos.*;
import com.qflow.appointment.service.AppointmentService;
import com.qflow.common.constants.HeaderNames;
import com.qflow.common.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_USER_ID) Long userId,
            @Valid @RequestBody BookRequest req) {
        return service.book(tenantId, userId, req);
    }

    @GetMapping("/my")
    public List<AppointmentResponse> mine(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_USER_ID) Long userId) {
        return service.mine(tenantId, userId);
    }

    @GetMapping
    public List<AppointmentResponse> forQueue(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_ROLE) String role,
            @RequestParam Long queueId) {
        Roles.require(role, Roles.ADMIN, Roles.STAFF);
        return service.forQueue(tenantId, queueId);
    }

    @GetMapping("/slots")
    public List<SlotView> slots(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestParam Long queueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.slots(tenantId, queueId, date);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_USER_ID) Long userId,
            @RequestHeader(HeaderNames.X_ROLE) String role,
            @PathVariable Long id) {
        return service.cancel(tenantId, userId, role, id);
    }

    @PostMapping("/{id}/check-in")
    public AppointmentResponse checkIn(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_USER_ID) Long userId,
            @RequestHeader(HeaderNames.X_ROLE) String role,
            @PathVariable Long id) {
        Roles.require(role, Roles.ADMIN, Roles.STAFF);
        return service.checkIn(tenantId, userId, id);
    }
}