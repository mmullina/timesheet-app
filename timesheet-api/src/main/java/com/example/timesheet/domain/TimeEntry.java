package com.example.timesheet.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity 
@Table(name = "time_entry", indexes = @Index(name = "ix_time_entry_user_date", columnList = "user_id, work_date"))
public class TimeEntry {
    
    public static final int INCREMENT_MINUTES = 15;
    public static final int MAX_MINUTES = 24 * 60;

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private int minutes;

    @Column(length = 500)
    private String note;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version 
    private long version;

    protected TimeEntry() {}

    public TimeEntry(AppUser user, Task task, LocalDate workDate, int minutes, String note) {
        validateMinutes(minutes);
        this.user = user;
        this.task = task;
        this.workDate = workDate;
        this.minutes = minutes;
        this.note = note;
    }

    public static void validateMinutes(int minutes) {
        if (minutes <= 0 || minutes > MAX_MINUTES || minutes % INCREMENT_MINUTES != 0) {
            throw new IllegalArgumentException(
                "minutes must be a positive multiple of " + INCREMENT_MINUTES + 
                " and at most " + MAX_MINUTES
            );
        }
    }

    public void update(Task task, LocalDate workDate, int minutes, String note) {
        validateMinutes(minutes);
        this.task = task;
        this.workDate = workDate;
        this.minutes = minutes;
        this.note = note;
    }

    public void softDelete(Instant now) {
        if (deletedAt == null) {
            this.deletedAt = now;
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Task getTask() {
        return task;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public int getMinutes() {
        return minutes;
    }

    public String getNote() {
        return note;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Long getVersion() {
        return version;
    }
}
