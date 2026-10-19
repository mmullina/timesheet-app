package com.example.timesheet.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity 
@Table(name = "timesheet", uniqueConstraints = @UniqueConstraint(name = "uq_timesheet_user_week", columnNames = {"user_id", "week_start"}))
public class TimeSheet {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeSheetStatus status = TimeSheetStatus.OPEN;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Version 
    private long version;

    protected TimeSheet() {}

    public TimeSheet(AppUser user, LocalDate weekStart) {
        if (!Weeks.isWeekStart(weekStart)) {
            throw new IllegalArgumentException("week start must be a Monday " + weekStart);
        }
        this.user = user;
        this.weekStart = weekStart;
    }

    public void submit(Instant now) {
        if (status == TimeSheetStatus.SUBMITTED) {
            throw new IllegalStateException("Timesheet is already submitted");
        }
        this.status = TimeSheetStatus.SUBMITTED;
        this.submittedAt = now;
    }

    public void reopen() {
        if (status == TimeSheetStatus.OPEN) {
            throw new IllegalStateException("Timesheet is already open");
        }
        this.status = TimeSheetStatus.OPEN;
        this.submittedAt = null;
    }

    public boolean isSubmitted() {
        return status == TimeSheetStatus.SUBMITTED;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public TimeSheetStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Long getVersion() {
        return version;
    }
}
