package com.qflow.common.model;

public record QueueEvent(
        EventType type,
        Long tenantId,
        Long queueId,
        String queueName,
        Long tokenId,
        Integer tokenNumber,
        String citizenName,
        String phone,
        Integer counterNo,
        Integer position,
        Integer estimatedWaitMinutes,
        long occurredAt) {
}