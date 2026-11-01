package com.example.minimaltodo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.minimaltodo.reminder.ReminderScheduler;
import com.example.minimaltodo.util.JsonExporter;
import com.example.minimaltodo.util.Prefs;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Manages theme mode, completed task visibility, permission shortcuts, and data actions.
 */
public class SettingsActivity extends AppCompatActivity {

    private TaskViewModel viewModel;
    private MaterialButton exactAlarmButton;

    /**
     * Lets the user pick a file for the JSON export before the activity is started.
     */
    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),
                    uri -> {
                        if (uri != null) {
                            exportTo(uri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }
        toolbar.setNavigationOnClickListener(view -> finish());

        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        exactAlarmButton = findViewById(R.id.button_exact_alarm);
        exactAlarmButton.setOnClickListener(view -> openExactAlarmSettings());
        findViewById(R.id.button_notifications).setOnClickListener(view -> openNotificationSettings());

        RadioGroup themeGroup = findViewById(R.id.theme_group);
        // Select the saved theme before adding the listener so setup doesn't write it back.
        int mode = Prefs.getThemeMode(this);
        themeGroup.check(mode == Prefs.THEME_LIGHT ? R.id.theme_light
                : mode == Prefs.THEME_DARK ? R.id.theme_dark : R.id.theme_system);
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int selected = checkedId == R.id.theme_light ? Prefs.THEME_LIGHT
                    : checkedId == R.id.theme_dark ? Prefs.THEME_DARK
                    : Prefs.THEME_SYSTEM;
            Prefs.setThemeMode(this, selected);
            // applyTheme recreates the activity so the new theme takes effect immediately.
            Prefs.applyTheme(this);
        });

        MaterialSwitch showCompletedSwitch = findViewById(R.id.switch_show_completed);
        showCompletedSwitch.setChecked(Prefs.isShowCompleted(this));
        showCompletedSwitch.setOnCheckedChangeListener((buttonView, checked) ->
                Prefs.setShowCompleted(this, checked));

        findViewById(R.id.button_export).setOnClickListener(view ->
                exportLauncher.launch("todo-" + LocalDate.now() + ".json"));
        findViewById(R.id.button_clear_completed).setOnClickListener(view ->
                new MaterialAlertDialogBuilder(this)
                        .setMessage(R.string.clear_completed_confirm)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.confirm_clear, (dialog, which) -> {
                            viewModel.clearCompleted();
                            Snackbar.make(findViewById(R.id.settings_root),
                                    R.string.cleared, Snackbar.LENGTH_SHORT).show();
                        })
                        .show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Refreshes the exact alarm entry, since the permission may have changed in system settings.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (exactAlarmButton != null) {
            boolean supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
            exactAlarmButton.setVisibility(supported ? android.view.View.VISIBLE : android.view.View.GONE);
            if (supported) {
                boolean allowed = ReminderScheduler.canScheduleExactAlarms(this);
                exactAlarmButton.setText(allowed ? R.string.exact_alarm_on : R.string.exact_alarm_off);
            }
        }
    }

    /**
     * Opens the exact alarm permission screen, falling back to notification settings if unavailable.
     */
    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        if (ReminderScheduler.canScheduleExactAlarms(this)) {
            Snackbar.make(findViewById(R.id.settings_root),
                    R.string.exact_alarm_hint, Snackbar.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivity(intent);
        } catch (Exception e) {
            openNotificationSettings();
        }
    }

    /** Opens this app's notification settings, silently ignoring unsupported devices. */
    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            // No notification settings page on this device.
        }
    }

    /**
     * Writes all tasks as JSON to the chosen URI on a background thread.
     */
    private void exportTo(Uri uri) {
        viewModel.getAll(tasks -> new Thread(() -> {
            boolean success = true;
            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output == null) {
                    throw new IllegalStateException("Cannot open the selected file");
                }
                String json = JsonExporter.toJson(tasks);
                output.write(json.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                success = false;
            }

            boolean result = success;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Snackbar.make(findViewById(R.id.settings_root),
                        result ? R.string.exported : R.string.export_failed,
                        Snackbar.LENGTH_SHORT).show();
            });
        }).start());
    }
}
