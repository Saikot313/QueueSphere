package com.qflow.queue.service;

import com.qflow.common.util.TimeUtil;
import com.qflow.queue.entity.QueueToken;
import com.qflow.queue.entity.ServiceQueue;
import com.qflow.queue.entity.TokenStatus;
import com.qflow.queue.repository.QueueTokenRepository;
import com.qflow.queue.repository.ServiceQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Rebuilds live queue state in Redis from PostgreSQL after a Redis restart /
 * service restart.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStateRebuilder {

    private final ServiceQueueRepository queueRepo;
    private final QueueTokenRepository tokenRepo;
    private final StringRedisTemplate redis;

    @EventListener(ApplicationReadyEvent.class)
    public void rebuild() {
        try {
            LocalDate today = TimeUtil.today();
            for (ServiceQueue q : queueRepo.findAll()) {
                int max = tokenRepo.findMaxTokenNumber(q.getId(), today);
                if (max > 0) {
                    redis.opsForValue().setIfAbsent(QueueService.seqKey(q.getId(), today),
                            String.valueOf(max), Duration.ofDays(2));
                }
            }
            int count = 0;
            for (QueueToken t : tokenRepo.findByStatusAndTokenDate(TokenStatus.WAITING, today)) {
                redis.opsForZSet().add(QueueService.waitingKey(t.getQueueId()),
                        String.valueOf(t.getId()), QueueService.score(t));
                count++;
            }
            log.info("Redis state rebuilt: {} waiting tokens restored", count);
        } catch (Exception e) {
            log.error("Redis rebuild failed", e);
        }
    }
}