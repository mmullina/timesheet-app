package com.example.timesheet.domain;

import jakarta.persistence.*;

@Entity 
@Table(name = "project", uniqueConstraints = @UniqueConstraint(name = "uq_project_code", columnNames = "code"))
public class Project {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private boolean active = true;

    protected Project() {}

    public Project(String code, String name, String clientName) {
        this.code = code;
        this.name = name;
        this.clientName = clientName;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getClientName() {
        return clientName;
    }

    public boolean isActive() {
        return active;
    }

    public void archive() {
        active = false;
    }
}
