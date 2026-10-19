package com.example.timesheet.security;

import com.example.timesheet.domain.AppUser;
import com.example.timesheet.domain.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AppUserPrincipal(Long id, String email, String name, String passwordHash,
                               Role role, boolean active) implements UserDetails {

    public static AppUserPrincipal from(AppUser user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getName(),
                user.getPasswordHash(), user.getRole(), user.isActive());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { 
        return passwordHash; 
    }

    @Override
    public String getUsername() { 
        return email; 
    }

    @Override
    public boolean isEnabled() { 
        return active; 
    }
}