package com.example.timesheet.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.timesheet.domain.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Full history of one record, oldest first */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(String entityType, Long entityId);

    List<AuditLog> findTop50ByOrderByIdDesc();
}
