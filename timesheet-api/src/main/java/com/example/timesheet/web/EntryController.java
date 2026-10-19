package com.example.timesheet.web;

import com.example.timesheet.repository.TaskRepository;
import com.example.timesheet.security.AppUserPrincipal;
import com.example.timesheet.service.EntryCommand;
import com.example.timesheet.service.TimeEntryService;
import com.example.timesheet.service.TimeSheetService;
import com.example.timesheet.web.Dtos.EntryDto;
import com.example.timesheet.web.Dtos.TaskDto;
import com.example.timesheet.web.Dtos.WeekDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EntryController {

    private final TimeEntryService entryService;
    private final TimeSheetService sheetService;
    private final TaskRepository taskRepository;

    public EntryController(TimeEntryService entryService, TimeSheetService sheetService, TaskRepository taskRepository) {
        this.entryService = entryService;
        this.sheetService = sheetService;
        this.taskRepository = taskRepository;
    }

    /** Everything the project/task picker needs, in one call. */
    @GetMapping("/tasks")
    public List<TaskDto> catalog() {
        return taskRepository.findAllActiveWithProject().stream().map(TaskDto::from).toList();
    }

    @GetMapping("/week")
    public WeekDto week(@AuthenticationPrincipal AppUserPrincipal me,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return WeekDto.from(sheetService.getWeek(me.id(), date));
    }

    @PostMapping("/week/submit")
    public WeekDto submit(@AuthenticationPrincipal AppUserPrincipal me,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        sheetService.submit(me.id(), date);
        return WeekDto.from(sheetService.getWeek(me.id(), date));
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public EntryDto create(@AuthenticationPrincipal AppUserPrincipal me,
                           @Valid @RequestBody EntryCommand command) {
        return EntryDto.from(entryService.create(me.id(), command));
    }

    @PutMapping("/entries/{id}")
    public EntryDto update(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
                           @Valid @RequestBody EntryCommand command) {
        return EntryDto.from(entryService.update(me.id(), id, command));
    }

    @DeleteMapping("/entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        entryService.delete(me.id(), id);
    }
}