package com.qflow.notification.controller;

import com.qflow.common.constants.HeaderNames;
import com.qflow.common.security.Roles;
import com.qflow.notification.entity.Notification;
import com.qflow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public List<Notification> latest(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_ROLE) String role) {
        Roles.require(role, Roles.ADMIN, Roles.STAFF);
        return service.latest(tenantId);
    }
}