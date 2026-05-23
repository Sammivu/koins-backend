package com.middleware.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.entity.AuditLog;
import com.middleware.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository repository;

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @Around("@annotation(audit)")
    public Object audit(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {

        Object result = null;
        String status = "SUCCESS";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth != null ? auth.getName() : null;
        String userId = null;

        try {

            result = joinPoint.proceed();

            return result;

        } catch (Exception ex) {
            status = "FAILED";
            throw ex;

        } finally {

            AuditLog logEntry = AuditLog.builder()
                            .userId(email)
                            .action(audit.action())
                            .entityType(audit.entityType())
                            .requestPayload(objectMapper.writeValueAsString(joinPoint.getArgs()))
                            .responsePayload(result == null ? null : objectMapper.writeValueAsString(result))
                            .ipAddress(request.getRemoteAddr())
                            .status(status)
                            .createdDate(LocalDateTime.now())
                            .build();

            repository.save(logEntry);
        }
    }
}