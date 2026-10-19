package com.example.timesheet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.timesheet.domain.Weeks;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest 
class WeeksTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    @Test
    void mondayIsItsOwnWeekStart() {
        assertThat(Weeks.startOf(MONDAY)).isEqualTo(MONDAY);
    }

    @Test
    void midWeekDateMapsBackToMonday() {
        assertThat(Weeks.startOf(LocalDate.of(2026, 10, 7))).isEqualTo(MONDAY);
    }

    @Test
    void sundayBelongsToTheWeekThatStartedSixDaysEarlier() {
        assertThat(Weeks.startOf(LocalDate.of(2026, 10, 11))).isEqualTo(MONDAY);
    }

    @Test
    void nextMondayStartsANewWeek() {
        assertThat(Weeks.startOf(LocalDate.of(2026, 10, 12))).isEqualTo(LocalDate.of(2026, 10, 12));
    }

    @Test
    void weekStartsAcrossAYearBoundary() {
        // Thursday 1 Jan 2026 belongs to the week starting Monday 29 Dec 2025.
        assertThat(Weeks.startOf(LocalDate.of(2026, 1, 1))).isEqualTo(LocalDate.of(2025, 12, 29));
    }
}