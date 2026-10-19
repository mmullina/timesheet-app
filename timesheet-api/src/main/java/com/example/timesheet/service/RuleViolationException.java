package com.example.timesheet.service;

/** no submitted week, more than 24 hours, archived tasks  */
public class RuleViolationException extends RuntimeException {
    public RuleViolationException(String message) {
        super(message);
    }
}
