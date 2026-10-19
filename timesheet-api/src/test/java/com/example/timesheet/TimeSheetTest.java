package com.example.timesheet;

import org.junit.jupiter.api.Test;

import com.example.timesheet.domain.AppUser;
import com.example.timesheet.domain.Role;
import com.example.timesheet.domain.TimeSheet;
import com.example.timesheet.domain.TimeSheetStatus;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeSheetTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
    private static final Instant NOW = Instant.parse("2026-10-09T20:00:00Z");

    private final AppUser user = new AppUser("alice@example.com", "Alice", "hash", Role.USER);

    @Test
    void startsOpen() {
        TimeSheet sheet = new TimeSheet(user, MONDAY);

        assertThat(sheet.getStatus()).isEqualTo(TimeSheetStatus.OPEN);
        assertThat(sheet.getSubmittedAt()).isNull();
    }

    @Test
    void rejectsAWeekStartThatIsNotAMonday() {
        assertThatThrownBy(() -> new TimeSheet(user, MONDAY.plusDays(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitRecordsStatusAndTime() {
        TimeSheet sheet = new TimeSheet(user, MONDAY);

        sheet.submit(NOW);

        assertThat(sheet.isSubmitted()).isTrue();
        assertThat(sheet.getSubmittedAt()).isEqualTo(NOW);
    }

    @Test
    void cannotSubmitTwice() {
        TimeSheet sheet = new TimeSheet(user, MONDAY);
        sheet.submit(NOW);

        assertThatThrownBy(() -> sheet.submit(NOW.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(sheet.getSubmittedAt()).isEqualTo(NOW);
    }

    @Test
    void reopenClearsTheSubmission() {
        TimeSheet sheet = new TimeSheet(user, MONDAY);
        sheet.submit(NOW);

        sheet.reopen();

        assertThat(sheet.isSubmitted()).isFalse();
        assertThat(sheet.getSubmittedAt()).isNull();
    }

    @Test
    void cannotReopenAnOpenSheet() {
        TimeSheet sheet = new TimeSheet(user, MONDAY);

        assertThatThrownBy(sheet::reopen).isInstanceOf(IllegalStateException.class);
    }
}