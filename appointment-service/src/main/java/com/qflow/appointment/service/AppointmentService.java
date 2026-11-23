package com.qflow.appointment.service;

import com.qflow.appointment.dto.AppointmentDtos.*;
import com.qflow.appointment.entity.Appointment;
import com.qflow.appointment.entity.AppointmentSlot;
import com.qflow.appointment.entity.AppointmentStatus;
import com.qflow.appointment.repository.AppointmentRepository;
import com.qflow.appointment.repository.SlotRepository;
import com.qflow.common.constants.HeaderNames;
import com.qflow.common.exception.ApiException;
import com.qflow.common.model.Priority;
import com.qflow.common.security.Roles;
import com.qflow.common.util.TimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final SlotRepository slotRepo;
    private final RestClient restClient;

    @Value("${app.slot.capacity:5}")
    private int capacity;
    @Value("${app.slot.minutes:30}")
    private int slotMinutes;
    @Value("${app.slot.open-hour:9}")
    private int openHour;
    @Value("${app.slot.close-hour:17}")
    private int closeHour;

    public AppointmentService(AppointmentRepository appointmentRepo, SlotRepository slotRepo,
            RestClient.Builder restClientBuilder) {
        this.appointmentRepo = appointmentRepo;
        this.slotRepo = slotRepo;
        this.restClient = restClientBuilder.build();
    }

    @Transactional
    public AppointmentResponse book(Long tenantId, Long userId, BookRequest r) {
        LocalDateTime slot = r.slotTime();
        if (slot.isBefore(TimeUtil.now())) {
            throw ApiException.badRequest("Slot time must be in the future");
        }
        if (slot.getSecond() != 0 || slot.getNano() != 0 || slot.getMinute() % slotMinutes != 0) {
            throw ApiException.badRequest("Slot must align to " + slotMinutes + " minute boundaries");
        }
        if (slot.getHour() < openHour || slot.getHour() >= closeHour) {
            throw ApiException.badRequest("Working hours are " + openHour + ":00 - " + closeHour + ":00");
        }

        slotRepo.insertIfAbsent(tenantId, r.queueId(), slot, capacity);
        AppointmentSlot s = slotRepo.findForUpdate(tenantId, r.queueId(), slot)
                .orElseThrow(() -> ApiException.conflict("Slot unavailable"));
        if (s.getBooked() >= s.getCapacity()) {
            throw ApiException.conflict("This slot is full");
        }
        s.setBooked(s.getBooked() + 1);

        Appointment a = new Appointment();
        a.setTenantId(tenantId);
        a.setQueueId(r.queueId());
        a.setUserId(userId);
        a.setCitizenName(r.citizenName());
        a.setPhone(r.phone());
        a.setSlotTime(slot);
        a.setPriority(r.priority() == null ? Priority.NORMAL : r.priority());
        a.setStatus(AppointmentStatus.BOOKED);
        a.setCreatedAt(TimeUtil.now());
        return toResponse(appointmentRepo.save(a));
    }

    @Transactional
    public AppointmentResponse cancel(Long tenantId, Long userId, String role, Long id) {
        Appointment a = find(tenantId, id);
        boolean staff = Roles.ADMIN.equals(role) || Roles.STAFF.equals(role);
        if (!staff && !a.getUserId().equals(userId)) {
            throw ApiException.forbidden("You can only cancel your own appointment");
        }
        if (a.getStatus() != AppointmentStatus.BOOKED) {
            throw ApiException.conflict("Appointment is already " + a.getStatus());
        }
        slotRepo.findForUpdate(tenantId, a.getQueueId(), a.getSlotTime())
                .ifPresent(s -> s.setBooked(Math.max(0, s.getBooked() - 1)));
        a.setStatus(AppointmentStatus.CANCELLED);
        return toResponse(a);
    }

    /**
     * Check-in: calls queue-service (via Eureka) to issue a token with the
     * appointment's priority.
     */
    public AppointmentResponse checkIn(Long tenantId, Long staffUserId, Long id) {
        Appointment a = find(tenantId, id);
        if (a.getStatus() != AppointmentStatus.BOOKED) {
            throw ApiException.conflict("Appointment is already " + a.getStatus());
        }
        TokenView token;
        try {
            token = restClient.post()
                    .uri("http://queue-service/api/queues/{queueId}/tokens", a.getQueueId())
                    .header(HeaderNames.X_TENANT_ID, String.valueOf(tenantId))
                    .header(HeaderNames.X_USER_ID, String.valueOf(staffUserId))
                    .header(HeaderNames.X_ROLE, Roles.STAFF)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "citizenName", a.getCitizenName(),
                            "phone", a.getPhone() == null ? "" : a.getPhone(),
                            "priority", a.getPriority().name()))
                    .retrieve()
                    .body(TokenView.class);
        } catch (RestClientResponseException e) {
            throw new ApiException(502, "Queue service error: " + e.getStatusCode());
        }
        if (token == null) {
            throw new ApiException(502, "Queue service returned no token");
        }
        a.setTokenId(token.id());
        a.setTokenNumber(token.tokenNumber());
        a.setStatus(AppointmentStatus.CHECKED_IN);
        return toResponse(appointmentRepo.save(a));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> mine(Long tenantId, Long userId) {
        return appointmentRepo.findByTenantIdAndUserIdOrderBySlotTimeDesc(tenantId, userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> forQueue(Long tenantId, Long queueId) {
        return appointmentRepo.findByTenantIdAndQueueIdOrderBySlotTimeAsc(tenantId, queueId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SlotView> slots(Long tenantId, Long queueId, LocalDate date) {
        Map<LocalDateTime, AppointmentSlot> existing = slotRepo
                .findByTenantIdAndQueueIdAndSlotTimeBetween(tenantId, queueId,
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream().collect(Collectors.toMap(AppointmentSlot::getSlotTime, Function.identity()));

        List<SlotView> out = new ArrayList<>();
        LocalDateTime end = date.atTime(closeHour, 0);
        for (LocalDateTime t = date.atTime(openHour, 0); t.isBefore(end); t = t.plusMinutes(slotMinutes)) {
            AppointmentSlot s = existing.get(t);
            int cap = s != null ? s.getCapacity() : capacity;
            int booked = s != null ? s.getBooked() : 0;
            out.add(new SlotView(t, cap, Math.max(0, cap - booked)));
        }
        return out;
    }

    private Appointment find(Long tenantId, Long id) {
        return appointmentRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Appointment not found"));
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getQueueId(), a.getCitizenName(), a.getPhone(),
                a.getSlotTime(), a.getPriority(), a.getStatus(), a.getTokenId(), a.getTokenNumber());
    }
}