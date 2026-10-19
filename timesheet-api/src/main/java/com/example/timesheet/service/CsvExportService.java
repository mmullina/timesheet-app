package com.example.timesheet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.timesheet.domain.TimeEntry;
import com.example.timesheet.domain.TimeSheet;
import com.example.timesheet.domain.Weeks;
import com.example.timesheet.repository.TimeEntryRepository;
import com.example.timesheet.repository.TimeSheetRepository;

@Service 
public class CsvExportService {
    
    private record Key(Long userId, LocalDate weekStart) {
        
    }

    private final TimeSheetRepository sheetRepository;
    private final TimeEntryRepository timeEntryRepository;

    public CsvExportService(TimeSheetRepository sheetRepository, TimeEntryRepository timeEntryRepository) {
        this.sheetRepository = sheetRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    @Transactional(readOnly = true)
    public String exportSubmittedWeeks(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new RuleViolationException("The end of the period is before its start.");
        }
        LocalDate firstWeek = Weeks.startOf(from);
        LocalDate lastWeek = Weeks.startOf(to);

        Map<Key, Long> minutes = new HashMap<>();
        for (TimeEntry entry : timeEntryRepository.findLiveInPeriod(firstWeek, lastWeek.plusDays(6))) {
            minutes.merge(new Key(entry.getUser().getId(), Weeks.startOf(entry.getWorkDate())),
                    (long) entry.getMinutes(), Long::sum);
        }

        Comparator<TimeSheet> order = Comparator
                .comparing((TimeSheet s) -> s.getUser().getName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(s -> s.getUser().getId())
                .thenComparing(TimeSheet::getWeekStart);

        List<TimeSheet> submitted = sheetRepository.findByWeekStartBetween(firstWeek, lastWeek).stream()
                .filter(TimeSheet::isSubmitted)
                .sorted(order)
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append(line("name", "week_start", "hours"));
        for (TimeSheet sheet : submitted) {
            long total = minutes.getOrDefault(
                    new Key(sheet.getUser().getId(), sheet.getWeekStart()), 0L);
            csv.append(line(sheet.getUser().getName(), sheet.getWeekStart().toString(), hours(total)));
        }
        return csv.toString();
    }

    private static String line(String... cells) {
        String[] escaped = new String[cells.length];
        for (int i = 0; i < cells.length; i++) {
            escaped[i] = cell(cells[i]);
        }
        return String.join(",", escaped) + "\r\n";
    }

    private static String cell(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value; // stop spreadsheets from running it as a formula
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            value = "\"" + value.replace("\"", "\"\"") + "\""; // quote it, doubling inner quotes
        }
        return value;
    }

    private static String hours(long minutes) {
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .toPlainString();
    }
}
