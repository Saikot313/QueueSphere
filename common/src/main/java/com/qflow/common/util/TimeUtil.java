package com.qflow.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class TimeUtil {
    private TimeUtil() {
    }

    public static final ZoneId ZONE = ZoneId.of("Asia/Dhaka");

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}