package com.example.minimaltodo.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Stores the app settings: theme mode, whether the notification prompt was shown,
 * and whether completed tasks are listed.
 */
public final class Prefs {

    /** Follow the system dark mode setting (default). */
    public static final int THEME_SYSTEM = 0;
    /** Always use the light theme. */
    public static final int THEME_LIGHT = 1;
    /** Always use the dark theme. */
    public static final int THEME_DARK = 2;

    private static final String FILE = "settings";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_NOTIFICATION_ASKED = "notification_asked";
    private static final String KEY_SHOW_COMPLETED = "show_completed";

    /** Utility class; not instantiable. */
    private Prefs() {
    }

    /** Returns the private preferences file used for all settings. */
    private static SharedPreferences get(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Returns the saved theme mode, defaulting to following the system. */
    public static int getThemeMode(Context context) {
        return get(context).getInt(KEY_THEME, THEME_SYSTEM);
    }

    /** Saves the theme mode; it takes effect on the next applyTheme call. */
    public static void setThemeMode(Context context, int mode) {
        get(context).edit().putInt(KEY_THEME, mode).apply();
    }

    /** Maps the theme mode to AppCompatDelegate and applies it app-wide. */
    public static void applyTheme(Context context) {
        int mode;
        switch (getThemeMode(context)) {
            case THEME_LIGHT:
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case THEME_DARK:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /** Returns whether the notification permission prompt has already been shown. */
    public static boolean wasNotificationAsked(Context context) {
        return get(context).getBoolean(KEY_NOTIFICATION_ASKED, false);
    }

    /** Records that the notification permission prompt has been shown. */
    public static void markNotificationAsked(Context context) {
        get(context).edit().putBoolean(KEY_NOTIFICATION_ASKED, true).apply();
    }

    /** Returns whether completed tasks are listed; true by default. */
    public static boolean isShowCompleted(Context context) {
        return get(context).getBoolean(KEY_SHOW_COMPLETED, true);
    }

    /** Saves whether completed tasks should be listed. */
    public static void setShowCompleted(Context context, boolean show) {
        get(context).edit().putBoolean(KEY_SHOW_COMPLETED, show).apply();
    }
}
