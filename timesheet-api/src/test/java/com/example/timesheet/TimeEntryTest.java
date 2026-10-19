package com.example.timesheet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.timesheet.domain.AppUser;
import com.example.timesheet.domain.Project;
import com.example.timesheet.domain.Role;
import com.example.timesheet.domain.Task;
import com.example.timesheet.domain.TimeEntry;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeEntryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 7);

    private final AppUser user = new AppUser("alice@example.com", "Alice", "hash", Role.USER);
    private final Task task = new Task(new Project("P1", "Website", "Acme"), "Development", true);

    @ParameterizedTest
    @ValueSource(ints = {15, 30, 60, 480, 1440})
    void acceptsPositiveMultiplesOfFifteenUpToADay(int minutes) {
        assertThatCode(() -> TimeEntry.validateMinutes(minutes)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {-15, 0, 1, 10, 20, 1455, 1500})
    void rejectsZeroNegativeOffIncrementAndOverADay(int minutes) {
        assertThatThrownBy(() -> TimeEntry.validateMinutes(minutes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsBadMinutes() {
        assertThatThrownBy(() -> new TimeEntry(user, task, DAY, 20, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateWithBadMinutesLeavesTheEntryUntouched() {
        TimeEntry entry = new TimeEntry(user, task, DAY, 60, "original");

        assertThatThrownBy(() -> entry.update(task, DAY.plusDays(1), 20, "changed"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(entry.getMinutes()).isEqualTo(60);
        assertThat(entry.getWorkDate()).isEqualTo(DAY);
        assertThat(entry.getNote()).isEqualTo("original");
    }

    @Test
    void softDeleteMarksTheEntryAndKeepsTheFirstTimestamp() {
        TimeEntry entry = new TimeEntry(user, task, DAY, 60, null);
        Instant first = Instant.parse("2026-10-07T15:00:00Z");

        entry.softDelete(first);
        entry.softDelete(first.plusSeconds(3600));

        assertThat(entry.isDeleted()).isTrue();
        assertThat(entry.getDeletedAt()).isEqualTo(first);
    }
}