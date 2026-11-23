package com.qflow.queue.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_queues")
@Getter
@Setter
@NoArgsConstructor
public class ServiceQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long tenantId;
    private String name;
    private String code;
    private boolean active = true;
    private int avgServiceSeconds = 300;

    /** Optimistic locking: protects the moving-average update from lost updates. */
    @Version
    private long version;

    private LocalDateTime createdAt;
}