package com.qflow.queue.service;

import com.qflow.common.exception.ApiException;
import com.qflow.common.model.EventType;
import com.qflow.common.model.Priority;
import com.qflow.common.model.QueueEvent;
import com.qflow.common.util.TimeUtil;
import com.qflow.queue.dto.QueueDtos.*;
import com.qflow.queue.entity.QueueToken;
import com.qflow.queue.entity.ServiceQueue;
import com.qflow.queue.entity.TokenStatus;
import com.qflow.queue.messaging.EventPublisher;
import com.qflow.queue.repository.QueueTokenRepository;
import com.qflow.queue.repository.ServiceQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private static final int NEAR_TURN_POSITION = 3;

    private final ServiceQueueRepository queueRepo;
    private final QueueTokenRepository tokenRepo;
    private final StringRedisTemplate redis;
    private final EventPublisher events;

    // ---------- Redis key helpers ----------

    public static String waitingKey(Long queueId) {
        return "queue:" + queueId + ":waiting";
    }

    public static String seqKey(Long queueId, LocalDate date) {
        return "queue:" + queueId + ":seq:" + date;
    }

    /** Priority first (lower rank wins), then FIFO by issue time. */
    public static double score(QueueToken t) {
        long millis = t.getIssuedAt().atZone(TimeUtil.ZONE).toInstant().toEpochMilli();
        return t.getPriority().getRank() * 1.0e13 + millis;
    }

    // ---------- Queue management ----------

    public QueueResponse createQueue(Long tenantId, CreateQueueRequest r) {
        if (queueRepo.existsByTenantIdAndCode(tenantId, r.code())) {
            throw ApiException.conflict("Queue code already exists");
        }
        ServiceQueue q = new ServiceQueue();
        q.setTenantId(tenantId);
        q.setName(r.name());
        q.setCode(r.code());
        if (r.avgServiceSeconds() != null && r.avgServiceSeconds() > 0) {
            q.setAvgServiceSeconds(r.avgServiceSeconds());
        }
        q.setCreatedAt(TimeUtil.now());
        return toQueueResponse(queueRepo.save(q));
    }

    public List<QueueResponse> listQueues(Long tenantId) {
        return queueRepo.findByTenantIdOrderByNameAsc(tenantId).stream().map(this::toQueueResponse).toList();
    }

    public QueueStatusResponse status(Long tenantId, Long queueId) {
        ServiceQueue q = getQueue(tenantId, queueId);
        Long waiting = redis.opsForZSet().zCard(waitingKey(queueId));
        Integer nowServing = tokenRepo
                .findFirstByQueueIdAndTokenDateAndStatusInOrderByCalledAtDesc(
                        queueId, TimeUtil.today(), List.of(TokenStatus.CALLED, TokenStatus.SERVING))
                .map(QueueToken::getTokenNumber).orElse(null);
        return new QueueStatusResponse(q.getId(), q.getName(), waiting == null ? 0 : waiting,
                nowServing, q.getAvgServiceSeconds());
    }

    // ---------- Token lifecycle ----------

    public TokenResponse issueToken(Long tenantId, Long queueId, IssueTokenRequest req) {
        ServiceQueue q = getQueue(tenantId, queueId);
        if (!q.isActive()) {
            throw ApiException.badRequest("Queue is not active");
        }
        LocalDate today = TimeUtil.today();

        // Atomic counter in Redis -> concurrency-safe token numbers (DB unique
        // constraint is the safety net)
        String seqKey = seqKey(queueId, today);
        Long seq = redis.opsForValue().increment(seqKey);
        if (seq == null) {
            throw new IllegalStateException("Redis sequence unavailable");
        }
        if (seq == 1L) {
            redis.expire(seqKey, Duration.ofDays(2));
        }

        QueueToken t = new QueueToken();
        t.setTenantId(tenantId);
        t.setQueueId(queueId);
        t.setTokenNumber(seq.intValue());
        t.setTokenDate(today);
        t.setCitizenName(req.citizenName());
        t.setPhone(req.phone());
        t.setPriority(req.priority() == null ? Priority.NORMAL : req.priority());
        t.setStatus(TokenStatus.WAITING);
        t.setIssuedAt(TimeUtil.now());
        t = tokenRepo.save(t);

        redis.opsForZSet().add(waitingKey(queueId), String.valueOf(t.getId()), score(t));

        Integer pos = position(t);
        Integer wait = estimateMinutes(pos, q.getAvgServiceSeconds());
        events.publish(event(EventType.TOKEN_ISSUED, q, t, pos, wait));
        return toTokenResponse(t, pos, wait);
    }

    public TokenResponse callNext(Long tenantId, Long queueId, int counterNo) {
        ServiceQueue q = getQueue(tenantId, queueId);
        String key = waitingKey(queueId);

        // ZPOPMIN is atomic: two counters can never get the same token
        ZSetOperations.TypedTuple<String> popped = redis.opsForZSet().popMin(key);
        if (popped == null || popped.getValue() == null) {
            throw ApiException.notFound("No waiting tokens in this queue");
        }
        QueueToken t = tokenRepo.findByIdAndTenantId(Long.valueOf(popped.getValue()), tenantId)
                .orElseThrow(() -> ApiException.notFound("Token not found"));
        t.setStatus(TokenStatus.CALLED);
        t.setCounterNo(counterNo);
        t.setCalledAt(TimeUtil.now());
        t = tokenRepo.save(t);

        events.publish(event(EventType.TOKEN_CALLED, q, t, null, 0));

        // Tell the citizen who is now 3rd in line that their turn is near
        Set<String> third = redis.opsForZSet().range(key, NEAR_TURN_POSITION - 1, NEAR_TURN_POSITION - 1);
        if (third != null && !third.isEmpty()) {
            tokenRepo.findByIdAndTenantId(Long.valueOf(third.iterator().next()), tenantId)
                    .ifPresent(nt -> events.publish(event(EventType.TOKEN_NEAR, q, nt, NEAR_TURN_POSITION,
                            estimateMinutes(NEAR_TURN_POSITION, q.getAvgServiceSeconds()))));
        }
        return toTokenResponse(t);
    }

    public TokenResponse getToken(Long tenantId, Long tokenId) {
        return toTokenResponse(findToken(tenantId, tokenId));
    }

    public List<TokenResponse> waitingList(Long tenantId, Long queueId) {
        ServiceQueue q = getQueue(tenantId, queueId);
        Set<String> ids = redis.opsForZSet().range(waitingKey(queueId), 0, 49);
        List<TokenResponse> out = new ArrayList<>();
        if (ids == null)
            return out;
        int pos = 1;
        for (String id : ids) {
            QueueToken t = tokenRepo.findByIdAndTenantId(Long.valueOf(id), tenantId).orElse(null);
            if (t != null) {
                out.add(toTokenResponse(t, pos, estimateMinutes(pos, q.getAvgServiceSeconds())));
            }
            pos++;
        }
        return out;
    }

    public TokenResponse start(Long tenantId, Long tokenId) {
        QueueToken t = findToken(tenantId, tokenId);
        expect(t, TokenStatus.CALLED);
        t.setStatus(TokenStatus.SERVING);
        t.setStartedAt(TimeUtil.now());
        return toTokenResponse(tokenRepo.save(t));
    }

    public TokenResponse complete(Long tenantId, Long tokenId) {
        QueueToken t = findToken(tenantId, tokenId);
        expect(t, TokenStatus.CALLED, TokenStatus.SERVING);
        var now = TimeUtil.now();
        var from = t.getStartedAt() != null ? t.getStartedAt() : t.getCalledAt();
        t.setStatus(TokenStatus.COMPLETED);
        t.setCompletedAt(now);
        t = tokenRepo.save(t);
        if (from != null) {
            updateAverage(t.getQueueId(), Duration.between(from, now).getSeconds());
        }
        return toTokenResponse(t);
    }

    public TokenResponse skip(Long tenantId, Long tokenId) {
        QueueToken t = findToken(tenantId, tokenId);
        expect(t, TokenStatus.CALLED);
        t.setStatus(TokenStatus.SKIPPED);
        return toTokenResponse(tokenRepo.save(t));
    }

    public TokenResponse cancel(Long tenantId, Long tokenId) {
        QueueToken t = findToken(tenantId, tokenId);
        expect(t, TokenStatus.WAITING);
        redis.opsForZSet().remove(waitingKey(t.getQueueId()), String.valueOf(t.getId()));
        t.setStatus(TokenStatus.CANCELLED);
        return toTokenResponse(tokenRepo.save(t));
    }

    // ---------- Wait-time prediction (EWMA) ----------

    private void updateAverage(Long queueId, long actualSeconds) {
        try {
            ServiceQueue q = queueRepo.findById(queueId).orElseThrow();
            int updated = (int) Math.round(0.3 * actualSeconds + 0.7 * q.getAvgServiceSeconds());
            q.setAvgServiceSeconds(Math.max(30, Math.min(3600, updated)));
            queueRepo.save(q);
        } catch (OptimisticLockingFailureException e) {
            log.warn("Skipped average update for queue {} due to concurrent update", queueId);
        }
    }

    // ---------- Helpers ----------

    private ServiceQueue getQueue(Long tenantId, Long queueId) {
        return queueRepo.findByIdAndTenantId(queueId, tenantId)
                .orElseThrow(() -> ApiException.notFound("Queue not found"));
    }

    private QueueToken findToken(Long tenantId, Long tokenId) {
        return tokenRepo.findByIdAndTenantId(tokenId, tenantId)
                .orElseThrow(() -> ApiException.notFound("Token not found"));
    }

    private void expect(QueueToken t, TokenStatus... allowed) {
        for (TokenStatus s : allowed) {
            if (t.getStatus() == s)
                return;
        }
        throw ApiException.conflict("Token is in state " + t.getStatus() + ", action not allowed");
    }

    /** 1-based position in the waiting line, or null if not waiting. */
    private Integer position(QueueToken t) {
        if (t.getStatus() != TokenStatus.WAITING)
            return null;
        Long rank = redis.opsForZSet().rank(waitingKey(t.getQueueId()), String.valueOf(t.getId()));
        return rank == null ? null : rank.intValue() + 1;
    }

    private Integer estimateMinutes(Integer position, int avgSeconds) {
        if (position == null)
            return null;
        int ahead = Math.max(0, position - 1);
        return (int) Math.ceil(ahead * avgSeconds / 60.0);
    }

    private QueueEvent event(EventType type, ServiceQueue q, QueueToken t, Integer pos, Integer wait) {
        return new QueueEvent(type, t.getTenantId(), q.getId(), q.getName(), t.getId(), t.getTokenNumber(),
                t.getCitizenName(), t.getPhone(), t.getCounterNo(), pos, wait, System.currentTimeMillis());
    }

    private QueueResponse toQueueResponse(ServiceQueue q) {
        return new QueueResponse(q.getId(), q.getName(), q.getCode(), q.isActive(), q.getAvgServiceSeconds());
    }

    private TokenResponse toTokenResponse(QueueToken t) {
        int avg = queueRepo.findById(t.getQueueId()).map(ServiceQueue::getAvgServiceSeconds).orElse(300);
        Integer pos = position(t);
        return toTokenResponse(t, pos, estimateMinutes(pos, avg));
    }

    private TokenResponse toTokenResponse(QueueToken t, Integer pos, Integer wait) {
        return new TokenResponse(t.getId(), t.getQueueId(), t.getTokenNumber(), t.getCitizenName(), t.getPhone(),
                t.getPriority(), t.getStatus(), t.getCounterNo(), pos, wait, t.getIssuedAt());
    }
}