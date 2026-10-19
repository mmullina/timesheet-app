package com.example.timesheet;

import com.example.timesheet.domain.AppUser;
import com.example.timesheet.domain.Project;
import com.example.timesheet.domain.Role;
import com.example.timesheet.domain.Task;
import com.example.timesheet.domain.TimeEntry;
import com.example.timesheet.domain.TimeSheet;
import com.example.timesheet.repository.TimeEntryRepository;
import com.example.timesheet.repository.TimeSheetRepository;
import com.example.timesheet.service.CsvExportService;
import com.example.timesheet.service.RuleViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.example.timesheet.Fixtures.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvExportServiceTest {

    private static final String HEADER = "name,week_start,hours\r\n";

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 7);
    private static final LocalDate NEXT_MONDAY = LocalDate.of(2026, 10, 12);
    private static final LocalDate NEXT_TUESDAY = LocalDate.of(2026, 10, 13);

    @Mock TimeSheetRepository timesheets;
    @Mock TimeEntryRepository entries;

    private CsvExportService service;
    private AppUser alice;
    private AppUser bob;
    private Task task;

    @BeforeEach
    void setUp() {
        service = new CsvExportService(timesheets, entries);
        alice = withId(new AppUser("alice@example.com", "Alice", "hash", Role.USER), 1L);
        bob = withId(new AppUser("bob@example.com", "Bob", "hash", Role.USER), 2L);
        task = withId(new Task(new Project("P1", "Website", "Acme"), "Development", true), 7L);
    }

    @Test
    void exportsOnlySubmittedWeeks() {
        givenPeriodEndingWeek(MONDAY,
                List.of(submitted(alice, MONDAY), open(bob, MONDAY)),
                List.of(entry(alice, MONDAY, 120), entry(alice, WEDNESDAY, 45), entry(bob, MONDAY, 60)));

        String csv = service.exportSubmittedWeeks(MONDAY, MONDAY);

        assertThat(csv).isEqualTo(HEADER + "Alice,2026-10-05,2.75\r\n");
    }

    @Test
    void aSubmittedWeekWithNothingLoggedShowsZeroHours() {
        givenPeriodEndingWeek(MONDAY, List.of(submitted(alice, MONDAY)), List.of());

        assertThat(service.exportSubmittedWeeks(MONDAY, MONDAY))
                .isEqualTo(HEADER + "Alice,2026-10-05,0.00\r\n");
    }

    @Test
    void totalsAreKeptPerUserAndPerWeekAndSortedByNameThenWeek() {
        givenPeriodEndingWeek(NEXT_MONDAY,
                List.of(submitted(bob, MONDAY), submitted(alice, NEXT_MONDAY), submitted(alice, MONDAY)),
                List.of(entry(alice, MONDAY, 60), entry(alice, NEXT_MONDAY, 30), entry(bob, MONDAY, 120)));

        String csv = service.exportSubmittedWeeks(MONDAY, NEXT_MONDAY);

        assertThat(csv.split("\r\n")).containsExactly(
                "name,week_start,hours",
                "Alice,2026-10-05,1.00",
                "Alice,2026-10-12,0.50",
                "Bob,2026-10-05,2.00");
    }

    @Test
    void thePeriodIsWidenedToWholeWeeks() {
        // Wednesday to the next Tuesday must query Monday 5th to Monday 12th (Sunday 18th for entries).
        givenPeriodEndingWeek(NEXT_MONDAY, List.of(), List.of());

        assertThat(service.exportSubmittedWeeks(WEDNESDAY, NEXT_TUESDAY)).isEqualTo(HEADER);
    }

    @Test
    void neutralizesFormulasInNames() {
        AppUser mallory = withId(
                new AppUser("m@example.com", "=HYPERLINK(\"http://evil\")", "hash", Role.USER), 9L);
        givenPeriodEndingWeek(MONDAY, List.of(submitted(mallory, MONDAY)), List.of());

        assertThat(service.exportSubmittedWeeks(MONDAY, MONDAY))
                .contains("\"'=HYPERLINK(\"\"http://evil\"\")\",2026-10-05,0.00");
    }

    @Test
    void rejectsAPeriodThatEndsBeforeItStarts() {
        assertThatThrownBy(() -> service.exportSubmittedWeeks(NEXT_MONDAY, MONDAY))
                .isInstanceOf(RuleViolationException.class);
    }

    // ---- helpers -----------------------------------------------------------------------

    /** Stubs both reads for a period that runs from MONDAY to the end of the given last week. */
    private void givenPeriodEndingWeek(LocalDate lastWeek, List<TimeSheet> sheets, List<TimeEntry> periodEntries) {
        when(timesheets.findByWeekStartBetween(MONDAY, lastWeek)).thenReturn(sheets);
        when(entries.findLiveInPeriod(MONDAY, lastWeek.plusDays(6))).thenReturn(periodEntries);
    }

    private TimeEntry entry(AppUser user, LocalDate date, int minutes) {
        return new TimeEntry(user, task, date, minutes, null);
    }

    private TimeSheet submitted(AppUser user, LocalDate weekStart) {
        TimeSheet sheet = open(user, weekStart);
        sheet.submit(Instant.parse("2026-10-09T20:00:00Z"));
        return sheet;
    }

    private TimeSheet open(AppUser user, LocalDate weekStart) {
        return new TimeSheet(user, weekStart);
    }
}