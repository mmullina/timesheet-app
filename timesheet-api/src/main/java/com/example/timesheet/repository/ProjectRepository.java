package com.example.timesheet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.timesheet.domain.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    Optional<Project> findByCode(String code);

    List<Project> findByActiveTrueOrderByCodeAsc();
}
