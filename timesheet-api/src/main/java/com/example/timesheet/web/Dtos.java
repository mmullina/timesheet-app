package com.example.timesheet.web;

import com.example.timesheet.domain.Role;
import com.example.timesheet.domain.Task;
import com.example.timesheet.domain.TimeEntry;
import com.example.timesheet.domain.TimeSheetStatus;
import com.example.timesheet.service.TimeSheetService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class Dtos {

    private Dtos() {}

    public record EntryDto(Long id, Long taskId, String taskName, String projectCode, String projectName,
                           LocalDate workDate, int minutes, String note) {
        public static EntryDto from(TimeEntry entry) {
            Task task = entry.getTask();
            return new EntryDto(entry.getId(), task.getId(), task.getName(),
                    task.getProject().getCode(), task.getProject().getName(),
                    entry.getWorkDate(), entry.getMinutes(), entry.getNote());
        }
    }

    public record WeekDto(LocalDate weekStart, TimeSheetStatus status, Instant submittedAt,
                          long totalMinutes, List<EntryDto> entries) {
        public static WeekDto from(TimeSheetService.WeekView view) {
            return new WeekDto(view.weekStart(), view.status(), view.submittedAt(), view.totalMinutes(),
                    view.entries().stream().map(EntryDto::from).toList());
        }
    }

    public record TaskDto(Long id, String name, boolean billable,
                          String projectCode, String projectName, String clientName) {
        public static TaskDto from(Task task) {
            return new TaskDto(task.getId(), task.getName(), task.isBillable(),
                    task.getProject().getCode(), task.getProject().getName(),
                    task.getProject().getClientName());
        }
    }

    public record UserDto(Long id, String name, String email, Role role) {}
}