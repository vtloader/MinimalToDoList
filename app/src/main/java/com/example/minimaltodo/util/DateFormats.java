package com.example.minimaltodo.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formats due timestamps for the task list and the editor.
 */
public final class DateFormats {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("M月d日 HH:mm");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm");

    /** Utility class; not instantiable. */
    private DateFormats() {
    }

    /** Returns the short list label, prefixed with a today or tomorrow marker when applicable. */
    public static String formatDue(long dueAt) {
        LocalDateTime dateTime = toDateTime(dueAt);
        LocalDate today = LocalDate.now();
        LocalDate day = dateTime.toLocalDate();
        if (day.equals(today)) {
            return "今天 " + dateTime.format(TIME);
        }
        if (day.equals(today.plusDays(1))) {
            return "明天 " + dateTime.format(TIME);
        }
        if (day.getYear() == today.getYear()) {
            return dateTime.format(MONTH_DAY);
        }
        return dateTime.format(FULL);
    }

    /** Returns the full label used in the editor, always including the year. */
    public static String formatFull(long dueAt) {
        return toDateTime(dueAt).format(FULL);
    }

    /** Converts an epoch millisecond timestamp to a local date-time in the system zone. */
    private static LocalDateTime toDateTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }
}
