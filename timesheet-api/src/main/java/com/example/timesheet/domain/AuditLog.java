package com.example.timesheet.domain;

import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.*;

@Entity 
@Immutable 
@Table(name = "audit_log", indexes = @Index(name = "ix_audit_entity", columnList = "entity_type, entity_id"))
public class AuditLog {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    protected AuditLog() {}

    public AuditLog(Instant occurredAt, Long actorId, AuditAction action,
                    String entityType, Long entityId
    ) {
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public Long getId() { 
        return id; 
    }

    public Instant getOccurredAt() { 
        return occurredAt; 
    }

    public Long getActorId() { 
        return actorId;
    }

    public AuditAction getAction() { 
        return action; 
    }

    public String getEntityType() { 
        return entityType; 
    }

    public Long getEntityId() { 
        return entityId; 
    }
}
