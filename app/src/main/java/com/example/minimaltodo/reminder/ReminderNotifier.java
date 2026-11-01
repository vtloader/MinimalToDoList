package com.example.minimaltodo.reminder;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.example.minimaltodo.EditTaskActivity;
import com.example.minimaltodo.MainActivity;
import com.example.minimaltodo.R;
import com.example.minimaltodo.data.Task;
import com.example.minimaltodo.util.DateFormats;

/**
 * Builds, posts and cancels the reminder notifications.
 */
public final class ReminderNotifier {

    /** Utility class; not instantiable. */
    private ReminderNotifier() {
    }

    /** Posts a reminder notification for the task. */
    public static void notify(Context context, Task task) {
        // Notifications are dropped without this runtime permission on Android 13+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Show the due time, or a generic text when the task has none, then the note.
        String text = task.dueAt > 0
                ? DateFormats.formatDue(task.dueAt)
                : context.getString(R.string.notification_text);
        if (!task.note.isEmpty()) {
            text = text + " · " + task.note;
        }

        Intent openIntent = new Intent(context, EditTaskActivity.class)
                .putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id);
        // Add MainActivity first so Back from the editor returns to the list.
        PendingIntent contentIntent = TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, MainActivity.class))
                .addNextIntent(openIntent)
                .getPendingIntent(id(task.id),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                ReminderReceiver.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(task.title)
                .setContentText(text)
                // Expand long notes instead of truncating them.
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Tag the notification as a reminder so Do Not Disturb policies apply.
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                // Dismiss it once the user taps it.
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManagerCompat.from(context).notify(id(task.id), builder.build());
    }

    /** Cancels the reminder notification of the task. */
    public static void cancel(Context context, long taskId) {
        if (taskId <= 0) {
            return;
        }
        NotificationManagerCompat.from(context).cancel(id(taskId));
    }

    /** Derives the notification id from the task id, matching the alarm request code. */
    private static int id(long taskId) {
        return (int) (taskId & 0x7FFFFFFF);
    }
}
