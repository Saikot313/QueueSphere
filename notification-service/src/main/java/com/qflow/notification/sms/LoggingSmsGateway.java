package com.qflow.notification.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingSmsGateway implements SmsGateway {

    @Override
    public boolean send(String phone, String message) {
        log.info("[SMS -> {}] {}", phone, message);
        return true;
    }
}