package com.example.minimaltodo.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.minimaltodo.data.Task;

/**
 * Registers and cancels the AlarmManager entry that delivers a task reminder.
 */
public final class ReminderScheduler {

    /** Action of the reminder broadcast. */
    public static final String ACTION_REMIND = "com.example.minimaltodo.ACTION_REMIND";
    /** Intent extra that carries the task id. */
    public static final String EXTRA_TASK_ID = "task_id";

    /** Utility class; not instantiable. */
    private ReminderScheduler() {
    }

    /**
     * Schedules a reminder for the task, or skips it when no reminder is needed.
     */
    public static void schedule(Context context, Task task) {
        if (task == null || task.id <= 0 || !task.reminder || task.completed) {
            return;
        }
        if (task.dueAt <= System.currentTimeMillis()) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = createPendingIntent(context, task.id);
        try {
            // Exact alarms can be revoked by the user; fall back to an inexact one.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueAt, pendingIntent);
            } else {
                // Fire at the exact due time, waking the device from Doze if needed.
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueAt, pendingIntent);
            }
        } catch (SecurityException e) {
            // The permission may be revoked after the check, so fall back here too.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueAt, pendingIntent);
        }
    }

    /**
     * Cancels the reminder for the given task.
     */
    public static void cancel(Context context, long taskId) {
        if (taskId <= 0) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        // Same component, action and request code as when the alarm was registered.
        Intent intent = new Intent(context, ReminderReceiver.class).setAction(ACTION_REMIND);
        // FLAG_NO_CREATE returns null when no matching PendingIntent exists.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(taskId),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            // Remove the alarm first, then release the PendingIntent itself.
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    /** Returns whether the app may schedule exact alarms; always true before Android 12. */
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    /** Builds the broadcast PendingIntent that delivers the reminder. */
    private static PendingIntent createPendingIntent(Context context, long taskId) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setAction(ACTION_REMIND)
                .putExtra(EXTRA_TASK_ID, taskId);
        return PendingIntent.getBroadcast(
                context,
                requestCode(taskId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /** Derives the PendingIntent request code from the task id. */
    private static int requestCode(long taskId) {
        // Keep the low 31 bits so the request code stays a non-negative int.
        return (int) (taskId & 0x7FFFFFFF);
    }
}
