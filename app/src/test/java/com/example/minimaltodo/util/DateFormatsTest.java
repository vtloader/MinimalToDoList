package com.example.minimaltodo.util;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for DateFormats, covering the relative and full due date labels.
 */
public class DateFormatsTest {

    /** Converts a local date-time to epoch millis in the system zone, like the class under test. */
    private static long millis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** Verifies that a task due today gets the relative today marker and its time. */
    @Test
    public void todayShowsRelativeLabel() {
        long due = millis(LocalDateTime.of(LocalDate.now(), LocalTime.of(18, 0)));
        assertEquals("今天 18:00", DateFormats.formatDue(due));
    }

    /** Verifies that a task due tomorrow gets the relative tomorrow marker and its time. */
    @Test
    public void tomorrowShowsRelativeLabel() {
        long due = millis(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(9, 30)));
        assertEquals("明天 09:30", DateFormats.formatDue(due));
    }

    /** Verifies that later dates drop the relative word and show month and day instead. */
    @Test
    public void laterDateShowsMonthAndDay() {
        long due = millis(LocalDateTime.of(LocalDate.now().plusDays(10), LocalTime.of(8, 5)));
        String label = DateFormats.formatDue(due);
        assertTrue(label.contains("08:05"));
        assertTrue(label.contains("日"));
    }

    /** Verifies that formatFull always includes the year and zero-padded time. */
    @Test
    public void fullFormatContainsYear() {
        long due = millis(LocalDateTime.of(2030, 1, 2, 3, 4));
        assertEquals("2030年1月2日 03:04", DateFormats.formatFull(due));
    }
}
