package com.example.timesheet.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public final class Weeks {
    private Weeks() {}

    public static LocalDate startOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static boolean isWeekStart(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.MONDAY;
    }
}
