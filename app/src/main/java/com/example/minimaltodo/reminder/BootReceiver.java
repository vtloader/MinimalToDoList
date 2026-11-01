package com.example.minimaltodo.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.minimaltodo.data.Task;
import com.example.minimaltodo.data.TaskRepository;

/**
 * Restores the reminder alarms after a reboot or an app update.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle only boot completion and self-update; ignore other broadcasts.
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        // Extend the broadcast lifetime until the database query returns.
        PendingResult pendingResult = goAsync();
        TaskRepository.get(context).getPendingReminders(tasks -> {
            // schedule() already skips completed tasks and tasks without a reminder.
            for (Task task : tasks) {
                ReminderScheduler.schedule(context, task);
            }
            pendingResult.finish();
        });
    }
}
