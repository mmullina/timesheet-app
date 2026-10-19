package com.example.timesheet.web;

import com.example.timesheet.domain.AuditLog;
import com.example.timesheet.repository.AuditLogRepository;
import com.example.timesheet.security.AppUserPrincipal;
import com.example.timesheet.service.AdminReportService;
import com.example.timesheet.service.AdminReportService.WeekTotal;
import com.example.timesheet.service.CsvExportService;
import com.example.timesheet.service.TimeSheetService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/** Every route under /api/admin requires the ADMIN role (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminReportService reportService;
    private final CsvExportService csvService;
    private final TimeSheetService sheetService;
    private final AuditLogRepository auditRepository;

    public AdminController(AdminReportService reportService, CsvExportService csvService,
                           TimeSheetService sheetService, AuditLogRepository auditRepository) {
        this.reportService  = reportService;
        this.csvService = csvService;
        this.sheetService = sheetService;
        this.auditRepository = auditRepository;
    }

    @GetMapping("/weeks")
    public List<WeekTotal> weeks(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.weekTotals(from, to);
    }

    @GetMapping("/weeks.csv")
    public ResponseEntity<String> weeksCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"timesheets-" + from + "-to-" + to + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvService.exportSubmittedWeeks(from, to));
    }

    @PostMapping("/users/{userId}/weeks/reopen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reopen(@AuthenticationPrincipal AppUserPrincipal admin, @PathVariable Long userId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        sheetService.reopen(admin.id(), userId, date);
    }

    @GetMapping("/audit")
    public List<AuditLog> audit() {
    return auditRepository.findTop50ByOrderByIdDesc();
}
}