package com.example.timesheet;

import com.example.timesheet.domain.AppUser;
import com.example.timesheet.domain.AuditAction;
import com.example.timesheet.domain.Project;
import com.example.timesheet.domain.Role;
import com.example.timesheet.domain.Task;
import com.example.timesheet.domain.TimeEntry;
import com.example.timesheet.domain.TimeSheet;
import com.example.timesheet.repository.AppUserRepository;
import com.example.timesheet.repository.TaskRepository;
import com.example.timesheet.repository.TimeEntryRepository;
import com.example.timesheet.repository.TimeSheetRepository;
import com.example.timesheet.service.AuditService;
import com.example.timesheet.service.EntryCommand;
import com.example.timesheet.service.NotFoundException;
import com.example.timesheet.service.RuleViolationException;
import com.example.timesheet.service.TimeEntryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.example.timesheet.Fixtures.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TASK_ID = 7L;
    private static final Long ENTRY_ID = 50L;

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 7);
    private static final LocalDate NEXT_MONDAY = LocalDate.of(2026, 10, 12);
    private static final LocalDate NEXT_WEDNESDAY = LocalDate.of(2026, 10, 14);

    @Mock TimeEntryRepository entries;
    @Mock TaskRepository tasks;
    @Mock TimeSheetRepository timesheets;
    @Mock AppUserRepository users;
    @Mock AuditService audit;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T15:00:00Z"), ZoneOffset.UTC);

    private TimeEntryService service;
    private AppUser alice;
    private Project project;
    private Task task;

    @BeforeEach
    void setUp() {
        service = new TimeEntryService(entries, tasks, timesheets, users, audit, clock);
        alice = withId(new AppUser("alice@example.com", "Alice", "hash", Role.USER), USER_ID);
        project = new Project("P1", "Website", "Acme");
        task = withId(new Task(project, "Development", true), TASK_ID);
    }

    // ---- create ------------------------------------------------------------------------

    @Test
    void createSavesTheEntryAndWritesAnAuditRow() {
        when(tasks.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(users.getReferenceById(USER_ID)).thenReturn(alice);

        service.create(USER_ID, new EntryCommand(TASK_ID, WEDNESDAY, 120, "kickoff"));

        ArgumentCaptor<TimeEntry> saved = ArgumentCaptor.forClass(TimeEntry.class);
        verify(entries).save(saved.capture());
        assertThat(saved.getValue().getMinutes()).isEqualTo(120);
        assertThat(saved.getValue().getWorkDate()).isEqualTo(WEDNESDAY);
        assertThat(saved.getValue().getUser()).isSameAs(alice);

        verify(audit).record(eq(USER_ID), eq(AuditAction.ENTRY_CREATED), eq("TimeEntry"),
                any());
    }

    @Test
    void createRejectsMinutesThatAreNotInIncrements() {
        assertThatThrownBy(() -> service.create(USER_ID, new EntryCommand(TASK_ID, WEDNESDAY, 20, null)))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(tasks, timesheets, audit);
        verify(entries, never()).save(any());
    }

    @Test
    void createRejectsAnUnknownTask() {
        assertThatThrownBy(() -> service.create(USER_ID, new EntryCommand(TASK_ID, WEDNESDAY, 60, null)))
                .isInstanceOf(NotFoundException.class);

        verify(entries, never()).save(any());
    }

    @Test
    void createRejectsAnArchivedProject() {
        project.archive();
        when(tasks.findById(TASK_ID)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.create(USER_ID, new EntryCommand(TASK_ID, WEDNESDAY, 60, null)))
                .isInstanceOf(RuleViolationException.class);

        verify(entries, never()).save(any());
    }

    @Test
    void createRejectsASubmittedWeek() {
        when(tasks.findById(TASK_ID)).thenReturn(Optional.of(task));
        givenSubmittedWeek(MONDAY);

        assertThatThrownBy(() -> service.create(USER_ID, new EntryCommand(TASK_ID, WEDNESDAY, 60, null)))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("submitted");

        verify(entries, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void createAllowsExactlyTwentyFourHoursInADay() {
        when(tasks.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(entries.sumMinutesForDay(USER_ID, WEDNESDAY, 0L)).thenReturn(1320L);

        service.create(USER_ID, new EntryCommand(TASK_ID, WEDNESDAY, 120, null));

        verify(entries).save(any(TimeEntry.class));
    }

    @Test
    void createRejectsGoingOverTwentyFourHoursInADay() {
        when(tasks.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(entries.sumMinutesForDay(USER_ID, WEDNESDAY, 0L)).thenReturn(1320L);

        assertThatThrownBy(() -> service.create(USER_ID, new EntryCommand(TASK_ID, WEDNESDAY, 135, null)))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("24 hours");

        verify(entries, never()).save(any());
        verifyNoInteractions(audit);
    }

    // ---- update ------------------------------------------------------------------------

    @Test
    void updateChangesTheEntryAndAuditsBeforeAndAfter() {
        TimeEntry entry = existingEntry(WEDNESDAY, 120);
        givenLiveEntry(entry);

        service.update(USER_ID, ENTRY_ID, new EntryCommand(TASK_ID, WEDNESDAY, 240, "kickoff"));

        assertThat(entry.getMinutes()).isEqualTo(240);
        verify(audit).record(eq(USER_ID), eq(AuditAction.ENTRY_UPDATED), eq("TimeEntry"), eq(ENTRY_ID));
    }

    @Test
    void updateOfSomeoneElsesEntryLooksLikeNotFound() {
        AppUser bob = withId(new AppUser("bob@example.com", "Bob", "hash", Role.USER), 2L);
        givenLiveEntry(withId(new TimeEntry(bob, task, WEDNESDAY, 120, null), ENTRY_ID));

        assertThatThrownBy(() -> service.update(USER_ID, ENTRY_ID, new EntryCommand(TASK_ID, WEDNESDAY, 60, null)))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(audit);
    }

    @Test
    void updateCannotMoveAnEntryOutOfASubmittedWeek() {
        TimeEntry entry = existingEntry(WEDNESDAY, 120);
        givenLiveEntry(entry);
        givenSubmittedWeek(MONDAY);

        assertThatThrownBy(() ->
                service.update(USER_ID, ENTRY_ID, new EntryCommand(TASK_ID, NEXT_WEDNESDAY, 120, "kickoff")))
                .isInstanceOf(RuleViolationException.class);

        assertThat(entry.getWorkDate()).isEqualTo(WEDNESDAY);
        verifyNoInteractions(audit);
    }

    @Test
    void updateCannotMoveAnEntryIntoASubmittedWeek() {
        TimeEntry entry = existingEntry(NEXT_WEDNESDAY, 120);
        givenLiveEntry(entry);
        when(timesheets.findByUserIdAndWeekStart(USER_ID, NEXT_MONDAY)).thenReturn(Optional.empty());
        givenSubmittedWeek(MONDAY);

        assertThatThrownBy(() ->
                service.update(USER_ID, ENTRY_ID, new EntryCommand(TASK_ID, WEDNESDAY, 120, "kickoff")))
                .isInstanceOf(RuleViolationException.class);

        assertThat(entry.getWorkDate()).isEqualTo(NEXT_WEDNESDAY);
        verifyNoInteractions(audit);
    }

    @Test
    void updateDoesNotCountTheEntryBeingEditedAgainstTheDailyLimit() {
        givenLiveEntry(existingEntry(WEDNESDAY, 120));
        // Everything else logged that day: 22 hours. The entry's own 2 hours are excluded by id.
        when(entries.sumMinutesForDay(USER_ID, WEDNESDAY, ENTRY_ID)).thenReturn(1320L);

        // 22h + 2h = 24h exactly: allowed.
        service.update(USER_ID, ENTRY_ID, new EntryCommand(TASK_ID, WEDNESDAY, 120, "kickoff"));

        // 22h + 2h15m: over.
        assertThatThrownBy(() ->
                service.update(USER_ID, ENTRY_ID, new EntryCommand(TASK_ID, WEDNESDAY, 135, "kickoff")))
                .isInstanceOf(RuleViolationException.class);
    }

    @Test
    void updateKeepingTheSameTaskIsAllowedEvenAfterItIsArchived() {
        project.archive();
        TimeEntry entry = existingEntry(WEDNESDAY, 120);
        givenLiveEntry(entry);

        service.update(USER_ID, ENTRY_ID, new EntryCommand(TASK_ID, WEDNESDAY, 180, "kickoff"));

        assertThat(entry.getMinutes()).isEqualTo(180);
    }

    // ---- delete ------------------------------------------------------------------------

    @Test
    void deleteSoftDeletesAndAuditsTheOldValues() {
        TimeEntry entry = existingEntry(WEDNESDAY, 120);
        givenLiveEntry(entry);

        service.delete(USER_ID, ENTRY_ID);

        assertThat(entry.isDeleted()).isTrue();
        assertThat(entry.getDeletedAt()).isEqualTo(clock.instant());
        verify(audit).record(eq(USER_ID), eq(AuditAction.ENTRY_DELETED), eq("TimeEntry"), eq(ENTRY_ID));
    }

    @Test
    void deleteIsRejectedInASubmittedWeek() {
        TimeEntry entry = existingEntry(WEDNESDAY, 120);
        givenLiveEntry(entry);
        givenSubmittedWeek(MONDAY);

        assertThatThrownBy(() -> service.delete(USER_ID, ENTRY_ID))
                .isInstanceOf(RuleViolationException.class);

        assertThat(entry.isDeleted()).isFalse();
        verifyNoInteractions(audit);
    }

    @Test
    void deleteOfAnAlreadyDeletedOrMissingEntryIsNotFound() {
        // findByIdAndDeletedAtIsNull returns empty for both cases.
        assertThatThrownBy(() -> service.delete(USER_ID, ENTRY_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- helpers -----------------------------------------------------------------------

    private TimeEntry existingEntry(LocalDate date, int minutes) {
        return withId(new TimeEntry(alice, task, date, minutes, "kickoff"), ENTRY_ID);
    }

    private void givenLiveEntry(TimeEntry entry) {
        when(entries.findByIdAndDeletedAtIsNull(ENTRY_ID)).thenReturn(Optional.of(entry));
    }

    private void givenSubmittedWeek(LocalDate weekStart) {
        TimeSheet sheet = new TimeSheet(alice, weekStart);
        sheet.submit(clock.instant());
        when(timesheets.findByUserIdAndWeekStart(USER_ID, weekStart)).thenReturn(Optional.of(sheet));
    }
}