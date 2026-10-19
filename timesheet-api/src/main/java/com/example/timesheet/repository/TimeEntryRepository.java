package com.example.timesheet.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.timesheet.domain.TimeEntry;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
    
    Optional<TimeEntry> findByIdAndDeletedAtIsNull(long id);

    /** users entries between two dates with task and project */
    @Query("""
            select e from TimeEntry e
            join fetch e.task t
            join fetch t.project
            where e.user.id = :userId
                and e.workDate >= :from and e.workDate <= :to
                and e.deletedAt is null
            order by e.workDate, e.id
            """)
    List<TimeEntry> findLiveForUser(@Param("userId") Long userId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);
    
    /**
     * Minutes already logged by a user on one day, for the 24-hour rule.
     * Pass the id of the entry being edited as excludeId so it doesn't count against itself,
     * or 0 when creating a new one.
     */
    @Query("""
            select coalesce(sum(e.minutes), 0L) from TimeEntry e
            where e.user.id = :userId
                and e.workDate = :date
                and e.deletedAt is null
                and e.id <> :excludeId
            """)
    long sumMinutesForDay(@Param("userId") Long userId,
                          @Param("date") LocalDate date,
                          @Param("excludeId") Long excludeId);

    /** Admin view and CSV - everyones live entries in a period in a single query */
    @Query("""
            select e from TimeEntry e
            join fetch e.user
            join fetch e.task t
            join fetch t.project
            where e.workDate >= :from and e.workDate <= :to
                and e.deletedAt is null
            order by e.user.name, e.workDate, e.id
            """)
    List<TimeEntry> findLiveInPeriod(@Param("from") LocalDate from,
                                     @Param("to") LocalDate to);
}
