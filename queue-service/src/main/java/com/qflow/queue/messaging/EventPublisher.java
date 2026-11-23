package com.qflow.queue.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qflow.common.constants.Topics;
import com.qflow.common.model.QueueEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    public void publish(QueueEvent event) {
        try {
            kafka.send(Topics.QUEUE_EVENTS, String.valueOf(event.queueId()), mapper.writeValueAsString(event));
        } catch (Exception ex) {
            // A notification failure must never break queue operations
            log.error("Failed to publish event {}", event.type(), ex);
        }
    }
}