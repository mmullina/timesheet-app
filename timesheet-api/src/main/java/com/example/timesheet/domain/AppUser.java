package com.example.timesheet.domain;

import jakarta.persistence.*;

@Entity 
@Table(name = "app+user", uniqueConstraints = @UniqueConstraint(name = "uq_app_user_email", columnNames = "email"))
public class AppUser {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    protected AppUser() {}

    public AppUser(String email, String name, String passwordHash, Role role) {
        this.email = email.trim().toLowerCase();
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void changePasswordHash(String hash) {
        this.passwordHash = hash;
    }
}
