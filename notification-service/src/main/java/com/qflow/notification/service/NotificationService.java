package com.qflow.notification.service;

import com.qflow.common.model.QueueEvent;
import com.qflow.common.util.TimeUtil;
import com.qflow.notification.entity.Notification;
import com.qflow.notification.repository.NotificationRepository;
import com.qflow.notification.sms.SmsGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository repo;
    private final SmsGateway smsGateway;

    public void handle(QueueEvent e) {
        if (e.phone() == null || e.phone().isBlank()) {
            log.info("No phone for token {}, skipping SMS", e.tokenId());
            return;
        }
        String message = buildMessage(e);
        boolean sent = smsGateway.send(e.phone(), message);

        Notification n = new Notification();
        n.setTenantId(e.tenantId());
        n.setTokenId(e.tokenId());
        n.setPhone(e.phone());
        n.setEventType(e.type().name());
        n.setMessage(message);
        n.setStatus(sent ? "SENT" : "FAILED");
        n.setCreatedAt(TimeUtil.now());
        repo.save(n);
    }

    public List<Notification> latest(Long tenantId) {
        return repo.findTop50ByTenantIdOrderByIdDesc(tenantId);
    }

    private String buildMessage(QueueEvent e) {
        return switch (e.type()) {
            case TOKEN_ISSUED -> String.format(
                    "%s: আপনার টোকেন নম্বর %d। আনুমানিক অপেক্ষা %s মিনিট।",
                    e.queueName(), e.tokenNumber(), e.estimatedWaitMinutes());
            case TOKEN_NEAR -> String.format(
                    "%s: টোকেন %d, আপনার পালা প্রায় এসে গেছে। আপনার সামনে আর ২ জন আছেন। অনুগ্রহ করে প্রস্তুত থাকুন।",
                    e.queueName(), e.tokenNumber());
            case TOKEN_CALLED -> String.format(
                    "%s: টোকেন %d, আপনাকে কাউন্টার %s এ ডাকা হয়েছে।",
                    e.queueName(), e.tokenNumber(), e.counterNo());
        };
    }
}