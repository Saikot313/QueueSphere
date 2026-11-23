package com.qflow.queue.entity;

import com.qflow.common.model.Priority;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "queue_tokens")
@Getter
@Setter
@NoArgsConstructor
public class QueueToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long tenantId;
    private Long queueId;
    private int tokenNumber;
    private LocalDate tokenDate;
    private String citizenName;
    private String phone;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private TokenStatus status;

    private Integer counterNo;
    private LocalDateTime issuedAt;
    private LocalDateTime calledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}