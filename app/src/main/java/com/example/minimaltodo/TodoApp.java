package com.example.minimaltodo;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import com.example.minimaltodo.reminder.ReminderReceiver;
import com.example.minimaltodo.util.Prefs;

/**
 * Application entry point. Applies the saved theme before any activity is
 * created and registers the notification channel used by {@link ReminderReceiver}.
 */
public class TodoApp extends Application {

    /** Applies the theme, then creates the reminder notification channel. */
    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.applyTheme(this);

        // Re-creating an existing channel leaves the user's settings untouched.
        NotificationChannel channel = new NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(getString(R.string.notification_channel_desc));
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
