package com.example.minimaltodo.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.minimaltodo.data.Task;
import com.example.minimaltodo.data.TaskRepository;

import java.util.List;

/**
 * Receives the reminder broadcast from AlarmManager and posts the notification.
 */
public class ReminderReceiver extends BroadcastReceiver {

    /** Notification channel id; must match the channel created at startup. */
    public static final String CHANNEL_ID = "task_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, 0);
        if (taskId <= 0) {
            return;
        }

        // goAsync() keeps the broadcast alive until finish() is called.
        PendingResult pendingResult = goAsync();
        TaskRepository.get(context).getAll(tasks -> {
            try {
                // The task may have been deleted or completed after the alarm was set.
                Task task = findTask(tasks, taskId);
                if (task != null && !task.completed) {
                    ReminderNotifier.notify(context, task);
                }
            } finally {
                // Always finish the broadcast, even if the lookup fails.
                pendingResult.finish();
            }
        });
    }

    /** Returns the task with the given id, or null when it no longer exists. */
    private static Task findTask(List<Task> tasks, long taskId) {
        for (Task task : tasks) {
            if (task.id == taskId) {
                return task;
            }
        }
        return null;
    }
}
