package com.example.timesheet.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.timesheet.domain.AuditAction;
import com.example.timesheet.domain.TimeEntry;
import com.example.timesheet.domain.TimeSheet;
import com.example.timesheet.domain.TimeSheetStatus;
import com.example.timesheet.domain.Weeks;
import com.example.timesheet.repository.AppUserRepository;
import com.example.timesheet.repository.TimeEntryRepository;
import com.example.timesheet.repository.TimeSheetRepository;

@Service 
public class TimeSheetService {
    
    private final static String ENTITY = "Timesheet";

    public record WeekView(LocalDate weekStart, TimeSheetStatus status, Instant submittedAt,
                            List<TimeEntry> entries, long totalMinutes) {

    }

    private final TimeSheetRepository sheetRepository;
    private final TimeEntryRepository timeRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;
    private final Clock clock;

    public TimeSheetService(TimeSheetRepository sheetRepository,
                            TimeEntryRepository timeRepository,
                            AppUserRepository userRepository,
                            AuditService auditService,
                            Clock clock) {
        this.sheetRepository = sheetRepository;
        this.timeRepository = timeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public WeekView getWeek(Long userId, LocalDate anyDateInWeek) {
        LocalDate weekStart = Weeks.startOf(anyDateInWeek);
        List<TimeEntry> weekEntries = timeRepository.findLiveForUser(userId, weekStart, weekStart.plusDays(6));
        long total = weekEntries.stream().mapToLong(TimeEntry::getMinutes).sum();

        // no rows = no submissions yet
        return sheetRepository.findByUserIdAndWeekStart(userId, weekStart)
                .map(sheet -> new WeekView(weekStart, sheet.getStatus(), sheet.getSubmittedAt(), weekEntries, total))
                .orElseGet(() -> new WeekView(weekStart, TimeSheetStatus.OPEN, null, weekEntries, total));
    }

    @Transactional
    public void submit(Long userId, LocalDate anyDateInWeek) {
        LocalDate weekStart = Weeks.startOf(anyDateInWeek);
        if (weekStart.isAfter(LocalDate.now(clock))) {
            throw new RuleViolationException("That week hasn't started yet.");
        }

        TimeSheet sheet = sheetRepository.findByUserIdAndWeekStart(userId, weekStart)
                .orElseGet(() -> new TimeSheet(userRepository.getReferenceById(userId), weekStart));
        if (sheet.isSubmitted()) {
            throw new RuleViolationException("The week of " + weekStart + " is already submitted.");
        }

        sheet.submit(clock.instant());
        sheetRepository.save(sheet);

        auditService.record(userId, AuditAction.TIMESHEET_SUBMITTED, ENTITY, sheet.getId());
    }

    @Transactional 
    public void reopen(Long actorId, Long userId, LocalDate anyDateInWeek) {
        LocalDate weekStart = Weeks.startOf(anyDateInWeek);
        TimeSheet sheet = sheetRepository.findByUserIdAndWeekStart(userId, weekStart)
                .orElseThrow(() -> new NotFoundException("No timesheet for the week of " + weekStart));
        
        if (!sheet.isSubmitted()) {
            throw new RuleViolationException("The week of " + weekStart + " isn't submitted.");
        }

        sheet.reopen();

        auditService.record(actorId, AuditAction.TIMESHEET_REOPENED, ENTITY, sheet.getId());
    }
}
