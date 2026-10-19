package com.example.timesheet.service;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EntryCommand(@NotNull Long taskId, 
                           @NotNull LocalDate workDate, 
                           int minutes, 
                           @Size(max = 500) String note) {
    
}
