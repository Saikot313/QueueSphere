package com.qflow.queue.controller;

import com.qflow.common.constants.HeaderNames;
import com.qflow.common.security.Roles;
import com.qflow.queue.dto.QueueDtos.*;
import com.qflow.queue.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService service;

    @PostMapping("/queues")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueResponse createQueue(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_ROLE) String role,
            @Valid @RequestBody CreateQueueRequest req) {
        Roles.require(role, Roles.ADMIN);
        return service.createQueue(tenantId, req);
    }

    @GetMapping("/queues")
    public List<QueueResponse> listQueues(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId) {
        return service.listQueues(tenantId);
    }

    @GetMapping("/queues/{queueId}/status")
    public QueueStatusResponse status(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @PathVariable Long queueId) {
        return service.status(tenantId, queueId);
    }

    @GetMapping("/queues/{queueId}/waiting")
    public List<TokenResponse> waiting(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_ROLE) String role,
            @PathVariable Long queueId) {
        Roles.require(role, Roles.ADMIN, Roles.STAFF);
        return service.waitingList(tenantId, queueId);
    }

    @PostMapping("/queues/{queueId}/tokens")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse issueToken(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @PathVariable Long queueId,
            @Valid @RequestBody IssueTokenRequest req) {
        return service.issueToken(tenantId, queueId, req);
    }

    @PostMapping("/queues/{queueId}/call-next")
    public TokenResponse callNext(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_ROLE) String role,
            @PathVariable Long queueId,
            @RequestParam(defaultValue = "1") int counter) {
        Roles.require(role, Roles.ADMIN, Roles.STAFF);
        return service.callNext(tenantId, queueId, counter);
    }

    @GetMapping("/tokens/{tokenId}")
    public TokenResponse getToken(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @PathVariable Long tokenId) {
        return service.getToken(tenantId, tokenId);
    }

    @PostMapping("/tokens/{tokenId}/start")
    public TokenResponse start(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_ROLE) String role,
            @PathVariable Long tokenId) {
        Roles.require(role, Roles.ADMIN, Roles.STAFF);
        return service.start(tenantId, tokenId);
    }

    @PostMapping("/tokens/{tokenId}/complete")
    public TokenResponse complete(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_ROLE) String role,
            @PathVariable Long tokenId) {
        Roles.require(role, Roles.ADMIN, Roles.STAFF);
        return service.complete(tenantId, tokenId);
    }

    @PostMapping("/tokens/{tokenId}/skip")
    public TokenResponse skip(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @RequestHeader(HeaderNames.X_ROLE) String role,
            @PathVariable Long tokenId) {
        Roles.require(role, Roles.ADMIN, Roles.STAFF);
        return service.skip(tenantId, tokenId);
    }

    @PostMapping("/tokens/{tokenId}/cancel")
    public TokenResponse cancel(@RequestHeader(HeaderNames.X_TENANT_ID) Long tenantId,
            @PathVariable Long tokenId) {
        return service.cancel(tenantId, tokenId);
    }
}