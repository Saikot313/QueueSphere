package com.qflow.queue.dto;

import com.qflow.common.model.Priority;
import com.qflow.queue.entity.TokenStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public final class QueueDtos {
    private QueueDtos() {
    }

    public record CreateQueueRequest(@NotBlank String name, @NotBlank String code, Integer avgServiceSeconds) {
    }

    public record IssueTokenRequest(@NotBlank String citizenName, String phone, Priority priority) {
    }

    public record QueueResponse(Long id, String name, String code, boolean active, int avgServiceSeconds) {
    }

    public record QueueStatusResponse(Long queueId, String queueName, long waiting,
            Integer nowServing, int avgServiceSeconds) {
    }

    public record TokenResponse(Long id, Long queueId, int tokenNumber, String citizenName, String phone,
            Priority priority, TokenStatus status, Integer counterNo,
            Integer position, Integer estimatedWaitMinutes, LocalDateTime issuedAt) {
    }
}