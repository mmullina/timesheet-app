package com.example.timesheet.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.timesheet.domain.AuditAction;
import com.example.timesheet.domain.AuditLog;
import com.example.timesheet.repository.AuditLogRepository;

import tools.jackson.databind.ObjectMapper;

@Service 
public class AuditService {
    
    private final AuditLogRepository auditRepository;
    private final Clock clock;

    public AuditService(AuditLogRepository auditRepository, ObjectMapper mapper, Clock clock) {
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long actorId, AuditAction action, String entityType, Long entityId) {
        auditRepository.save(new AuditLog(clock.instant(), actorId, action, entityType, entityId));
    }
}
