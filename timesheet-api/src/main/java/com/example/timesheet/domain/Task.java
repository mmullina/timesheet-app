package com.example.timesheet.domain;

import jakarta.persistence.*;

@Entity 
@Table(name = "task")
public class Task {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean billable = true;

    @Column(nullable = false)
    private boolean active = true;

    protected Task() {}

    public Task(Project project, String name, boolean billable) {
        this.project = project;
        this.name = name;
        this.billable = billable;
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public boolean isBillable() {
        return billable;
    }

    public boolean isActive() {
        return active;
    }

    public void archive() {
        active = false;
    }
}
