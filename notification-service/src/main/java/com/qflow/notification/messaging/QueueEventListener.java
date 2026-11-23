package com.qflow.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qflow.common.constants.Topics;
import com.qflow.common.model.QueueEvent;
import com.qflow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueEventListener {

    private final ObjectMapper mapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = Topics.QUEUE_EVENTS, groupId = "notification-service")
    public void onMessage(String payload) {
        try {
            QueueEvent event = mapper.readValue(payload, QueueEvent.class);
            notificationService.handle(event);
        } catch (Exception e) {
            log.error("Failed to process queue event: {}", payload, e);
        }
    }
}