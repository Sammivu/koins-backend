package com.middleware.service.impl;

import com.middleware.entity.AuditLog;
import com.middleware.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repository;

    public void log(
            UUID userId,
            String action,
            String entityType,
            String entityId,
            String request,
            String response,
            String ipAddress,
            String status
    ) {

        AuditLog log =
                AuditLog.builder()
                        .userId(
                                userId.toString()
                        )
                        .action(action)
                        .entityType(entityType)
                        .entityId(entityId)
                        .requestPayload(request)
                        .responsePayload(response)
                        .ipAddress(ipAddress)
                        .status(status)
                        .createdDate(
                                LocalDateTime.now()
                        )
                        .build();

        repository.save(log);
    }
}