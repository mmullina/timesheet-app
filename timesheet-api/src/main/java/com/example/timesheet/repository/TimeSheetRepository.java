package com.example.timesheet.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.timesheet.domain.TimeSheet;

public interface TimeSheetRepository extends JpaRepository<TimeSheet, Long> {

    /** Empty means week has not been touched which means open */
    Optional<TimeSheet> findByUserIdAndWeekStart(Long userId, LocalDate weekStart);
    
    /** Admint view - every users timesheet for the weeks in a period */
    @EntityGraph(attributePaths = "user")
    List<TimeSheet> findByWeekStartBetween(LocalDate from, LocalDate to);
}
