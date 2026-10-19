package com.example.timesheet.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.timesheet.domain.AuditAction;
import com.example.timesheet.domain.Task;
import com.example.timesheet.domain.TimeEntry;
import com.example.timesheet.domain.Weeks;
import com.example.timesheet.repository.AppUserRepository;
import com.example.timesheet.repository.TaskRepository;
import com.example.timesheet.repository.TimeEntryRepository;
import com.example.timesheet.repository.TimeSheetRepository;

@Service 
public class TimeEntryService {
    
    private static final String ENTITY = "TimeEntry";

    private final TimeEntryRepository timeRepository;
    private final TaskRepository taskRepository;
    private final TimeSheetRepository sheetRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;
    private final Clock clock;

    public TimeEntryService(TimeEntryRepository timeRepository,
                            TaskRepository taskRepository,
                            TimeSheetRepository sheetRepository,
                            AppUserRepository userRepository,
                            AuditService auditService,
                            Clock clock) {
        this.timeRepository = timeRepository;
        this.taskRepository = taskRepository;
        this.sheetRepository = sheetRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.clock = clock;                            
    }

    @Transactional 
    public TimeEntry create(Long userId, EntryCommand command) {
        TimeEntry.validateMinutes(command.minutes());
        Task task = requireUsableTask(command.taskId());
        assertWeekOpen(userId, command.workDate());
        assertDayCapacity(userId, command.workDate(), command.minutes(), 0L);

        TimeEntry entry = new TimeEntry(userRepository.getReferenceById(userId), task, command.workDate(), command.minutes(), command.note());
        timeRepository.save(entry);

        auditService.record(userId, AuditAction.ENTRY_CREATED, ENTITY, entry.getId());

        return entry;
    }

    @Transactional
    public TimeEntry update(Long userId, Long entryId, EntryCommand command) {
        TimeEntry entry = requireOwnLiveEntry(userId, entryId);
        TimeEntry.validateMinutes(command.minutes());

        Task task = command.taskId().equals(entry.getTask().getId())
                ? entry.getTask() : requireUsableTask(command.taskId());

        assertWeekOpen(userId, entry.getWorkDate());
        assertWeekOpen(userId, command.workDate());
        assertDayCapacity(userId, command.workDate(), command.minutes(), entryId);

        entry.update(task, command.workDate(), command.minutes(), command.note());

    
        auditService.record(userId, AuditAction.ENTRY_UPDATED, ENTITY, entryId);
        

        return entry;
    }

    @Transactional 
    public void delete(Long userId, Long entryId) {
        TimeEntry entry = requireOwnLiveEntry(userId, entryId);
        assertWeekOpen(userId, entry.getWorkDate());

        entry.softDelete(clock.instant());

        auditService.record(userId, AuditAction.ENTRY_DELETED, ENTITY, entryId);
    }

    private void assertWeekOpen(Long userId, LocalDate workDate) {
        LocalDate weekStart = Weeks.startOf(workDate);
        boolean submitted = sheetRepository.findByUserIdAndWeekStart(userId, weekStart)
                    .filter(sheet -> sheet.isSubmitted())
                    .isPresent();
        if (submitted) {
            throw new RuleViolationException(
                "The week of " + weekStart + " is submitted. Admin required to reopen"
            );
        }
    }

    private void assertDayCapacity(Long userId, LocalDate date, int minutes, Long excludeEntityId) {
        long alreadyLogged = timeRepository.sumMinutesForDay(userId, date, excludeEntityId);
        if (alreadyLogged + minutes > TimeEntry.MAX_MINUTES) {
            throw new RuleViolationException(
                "That would put " + date + " over 24 hours (" + alreadyLogged + " minutes already logged)."
            );
        }    
    }

    private Task requireUsableTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task " + taskId + " not found"));
        
        if (!task.isActive() || !task.getProject().isActive()) {
            throw new RuleViolationException("Project or task archived");
        }

        return task;
    }

    private TimeEntry requireOwnLiveEntry(Long userId, Long entryId) {
        return timeRepository.findByIdAndDeletedAtIsNull(entryId)
                .filter(entry -> entry.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Entry " + entryId + " not found"));
    }
}
