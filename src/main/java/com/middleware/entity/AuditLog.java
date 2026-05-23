package com.middleware.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    private String action;

    private String entityType;

    private String entityId;

    @Column(columnDefinition = "json")
    private String requestPayload;

    @Column(columnDefinition = "json")
    private String responsePayload;

    private String ipAddress;

    private String status;

    private LocalDateTime createdDate;
}