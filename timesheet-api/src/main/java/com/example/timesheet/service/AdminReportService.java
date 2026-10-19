package com.example.timesheet.service;

import com.example.timesheet.domain.AppUser;
import com.example.timesheet.domain.TimeEntry;
import com.example.timesheet.domain.TimeSheet;
import com.example.timesheet.domain.TimeSheetStatus;
import com.example.timesheet.domain.Weeks;
import com.example.timesheet.repository.AppUserRepository;
import com.example.timesheet.repository.TimeEntryRepository;
import com.example.timesheet.repository.TimeSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminReportService {

    private static final long MAX_PERIOD_DAYS = 366;

    /** One user's week: the unit the admin screen shows. */
    public record WeekTotal(Long userId, String userName, LocalDate weekStart,
                            TimeSheetStatus status, long totalMinutes) {}

    private record Key(Long userId, LocalDate weekStart) {}

    private final TimeEntryRepository entryRepository;
    private final TimeSheetRepository sheetRepository;
    private final AppUserRepository userRepository;

    public AdminReportService(TimeEntryRepository entryRepository, TimeSheetRepository sheetRepository,
                              AppUserRepository userRepository) {
        this.entryRepository = entryRepository;
        this.sheetRepository = sheetRepository;
        this.userRepository = userRepository;
    }

    /**
     * Every user's total for every week the period touches. The period is widened to whole
     * Monday-to-Sunday weeks, because a week is the unit that gets submitted. Active users
     * with nothing logged still get a row (OPEN, 0 minutes) so admins can see who is missing.
     * Inactive users appear only if they have data in the period.
     */
    @Transactional(readOnly = true)
    public List<WeekTotal> weekTotals(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new RuleViolationException("The end of the period is before its start.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_PERIOD_DAYS) {
            throw new RuleViolationException("Periods are limited to " + MAX_PERIOD_DAYS + " days.");
        }

        LocalDate firstWeek = Weeks.startOf(from);
        LocalDate lastWeek = Weeks.startOf(to);

        Map<Long, AppUser> people = new LinkedHashMap<>();
        for (AppUser user : userRepository.findByActiveTrueOrderByNameAsc()) {
            people.put(user.getId(), user);
        }

        Map<Key, Long> minutes = new HashMap<>();
        for (TimeEntry entry : entryRepository.findLiveInPeriod(firstWeek, lastWeek.plusDays(6))) {
            AppUser user = entry.getUser();
            people.putIfAbsent(user.getId(), user);
            minutes.merge(new Key(user.getId(), Weeks.startOf(entry.getWorkDate())),
                    (long) entry.getMinutes(), Long::sum);
        }

        Map<Key, TimeSheetStatus> statuses = new HashMap<>();
        for (TimeSheet sheet : sheetRepository.findByWeekStartBetween(firstWeek, lastWeek)) {
            AppUser user = sheet.getUser();
            people.putIfAbsent(user.getId(), user);
            statuses.put(new Key(user.getId(), sheet.getWeekStart()), sheet.getStatus());
        }

        List<AppUser> ordered = people.values().stream()
                .sorted(Comparator.comparing(AppUser::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AppUser::getId))
                .toList();

        List<WeekTotal> rows = new ArrayList<>();
        for (AppUser user : ordered) {
            for (LocalDate week = firstWeek; !week.isAfter(lastWeek); week = week.plusWeeks(1)) {
                Key key = new Key(user.getId(), week);
                rows.add(new WeekTotal(user.getId(), user.getName(), week,
                        statuses.getOrDefault(key, TimeSheetStatus.OPEN),
                        minutes.getOrDefault(key, 0L)));
            }
        }
        return rows;
    }
}