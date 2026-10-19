package com.example.timesheet;

import com.example.timesheet.domain.AppUser;
import com.example.timesheet.domain.AuditAction;
import com.example.timesheet.domain.Project;
import com.example.timesheet.domain.Role;
import com.example.timesheet.domain.Task;
import com.example.timesheet.domain.TimeEntry;
import com.example.timesheet.domain.TimeSheet;
import com.example.timesheet.domain.TimeSheetStatus;
import com.example.timesheet.repository.AppUserRepository;
import com.example.timesheet.repository.TimeEntryRepository;
import com.example.timesheet.repository.TimeSheetRepository;
import com.example.timesheet.service.AuditService;
import com.example.timesheet.service.NotFoundException;
import com.example.timesheet.service.RuleViolationException;
import com.example.timesheet.service.TimeSheetService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class TimesheetServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 99L;
    private static final Long SHEET_ID = 300L;

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 7);
    private static final LocalDate NEXT_WEDNESDAY = LocalDate.of(2026, 10, 14);

    @Mock TimeSheetRepository timesheets;
    @Mock TimeEntryRepository entries;
    @Mock AppUserRepository users;
    @Mock AuditService audit;

    // "Today" is Wednesday 7 October 2026.
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T15:00:00Z"), ZoneOffset.UTC);

    private TimeSheetService service;
    private AppUser alice;
    private Task task;

    @BeforeEach
    void setUp() {
        service = new TimeSheetService(timesheets, entries, users, audit, clock);
        alice = withId(new AppUser("alice@example.com", "Alice", "hash", Role.USER), USER_ID);
        task = withId(new Task(new Project("P1", "Website", "Acme"), "Development", true), 7L);
    }

    // ---- week view ---------------------------------------------------------------------

    @Test 
    void weekViewTotalsLiveEntriesAndDefaultsToOpen() {
        when(entries.findLiveForUser(USER_ID, MONDAY, MONDAY.plusDays(6))).thenReturn(List.of(
                new TimeEntry(alice, task, MONDAY, 120, null),
                new TimeEntry(alice, task, WEDNESDAY, 45, null)));

        // Any date inside the week selects the same Monday-to-Sunday window.
        TimeSheetService.WeekView view = service.getWeek(USER_ID, WEDNESDAY);

        assertThat(view.weekStart()).isEqualTo(MONDAY);
        assertThat(view.status()).isEqualTo(TimeSheetStatus.OPEN);
        assertThat(view.submittedAt()).isNull();
        assertThat(view.totalMinutes()).isEqualTo(165);
        assertThat(view.entries()).hasSize(2);
    }

    @Test
    void weekViewReportsASubmittedWeek() {
        TimeSheet sheet = new TimeSheet(alice, MONDAY);
        sheet.submit(clock.instant());
        when(timesheets.findByUserIdAndWeekStart(USER_ID, MONDAY)).thenReturn(Optional.of(sheet));

        TimeSheetService.WeekView view = service.getWeek(USER_ID, MONDAY);

        assertThat(view.status()).isEqualTo(TimeSheetStatus.SUBMITTED);
        assertThat(view.submittedAt()).isEqualTo(clock.instant());
    }

    // ---- submit ------------------------------------------------------------------------

    @Test
    void submitMarksTheWeekSubmittedAndAudits() {
        when(users.getReferenceById(USER_ID)).thenReturn(alice);

        service.submit(USER_ID, WEDNESDAY);

        ArgumentCaptor<TimeSheet> saved = ArgumentCaptor.forClass(TimeSheet.class);
        verify(timesheets).save(saved.capture());
        assertThat(saved.getValue().getWeekStart()).isEqualTo(MONDAY);
        assertThat(saved.getValue().isSubmitted()).isTrue();
        assertThat(saved.getValue().getSubmittedAt()).isEqualTo(clock.instant());

        verify(audit).record(eq(USER_ID), eq(AuditAction.TIMESHEET_SUBMITTED), eq("Timesheet"), any());
    }

    @Test
    void submitRejectsAWeekThatHasNotStarted() {
        assertThatThrownBy(() -> service.submit(USER_ID, NEXT_WEDNESDAY))
                .isInstanceOf(RuleViolationException.class);

        verify(timesheets, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void submitRejectsAnAlreadySubmittedWeek() {
        TimeSheet sheet = new TimeSheet(alice, MONDAY);
        sheet.submit(clock.instant());
        when(timesheets.findByUserIdAndWeekStart(USER_ID, MONDAY)).thenReturn(Optional.of(sheet));

        assertThatThrownBy(() -> service.submit(USER_ID, WEDNESDAY))
                .isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("already submitted");

        verifyNoInteractions(audit);
    }

    // ---- reopen ------------------------------------------------------------------------

    @Test
    void reopenOpensTheWeekAndAuditsTheAdminAsActor() {
        TimeSheet sheet = withId(new TimeSheet(alice, MONDAY), SHEET_ID);
        sheet.submit(clock.instant());
        when(timesheets.findByUserIdAndWeekStart(USER_ID, MONDAY)).thenReturn(Optional.of(sheet));

        service.reopen(ADMIN_ID, USER_ID, WEDNESDAY);

        assertThat(sheet.isSubmitted()).isFalse();
        assertThat(sheet.getSubmittedAt()).isNull();
        verify(audit).record(eq(ADMIN_ID), eq(AuditAction.TIMESHEET_REOPENED), eq("Timesheet"), eq(SHEET_ID));
    }

    @Test
    void reopenRejectsAWeekThatIsNotSubmitted() {
        when(timesheets.findByUserIdAndWeekStart(USER_ID, MONDAY))
                .thenReturn(Optional.of(new TimeSheet(alice, MONDAY)));

        assertThatThrownBy(() -> service.reopen(ADMIN_ID, USER_ID, MONDAY))
                .isInstanceOf(RuleViolationException.class);

        verifyNoInteractions(audit);
    }

    @Test 
    void reopenOfAWeekWithNoTimesheetIsNotFound() {
        assertThatThrownBy(() -> service.reopen(ADMIN_ID, USER_ID, MONDAY))
                .isInstanceOf(NotFoundException.class);
    }
}