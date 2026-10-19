package com.example.timesheet;

import org.springframework.test.util.ReflectionTestUtils;

public final class Fixtures {

    private Fixtures() {}

    /** The database assigns ids, so entities have no setter. Tests set the field directly. */
    public static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}