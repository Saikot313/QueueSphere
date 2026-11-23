package com.qflow.notification.sms;

public interface SmsGateway {
    /** @return true if the SMS was accepted by the provider */
    boolean send(String phone, String message);
}