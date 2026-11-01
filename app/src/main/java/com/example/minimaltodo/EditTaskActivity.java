package com.example.minimaltodo;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.minimaltodo.data.Task;
import com.example.minimaltodo.util.DateFormats;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Creates or edits a task's title, note, category, due date, and reminder.
 */
public class EditTaskActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";

    private TaskViewModel viewModel;
    private TextInputLayout titleLayout;
    private TextInputEditText titleInput;
    private TextInputEditText noteInput;
    private MaterialAutoCompleteTextView categoryInput;
    private MaterialButton dueButton;
    private MaterialButton clearDueButton;
    private MaterialSwitch reminderSwitch;

    private long taskId;
    private Task task;
    /** Set once the editable task is available, so it is populated only once. */
    private boolean loaded;
    /** Set while a save is in flight to block duplicate inserts. */
    private boolean saving;
    /** True when the activity is being restored by the system. */
    private boolean restoring;
    /** Due time waiting to be restored, or -1 when there is none. */
    private long restoredDueAt = -1;
    private boolean restoredReminder;

    private static final String STATE_DUE_AT = "state_due_at";
    private static final String STATE_REMINDER = "state_reminder";

    /**
     * Sets up the form for a new task or an existing one loaded by id.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, 0);
        restoring = savedInstanceState != null;
        setTitle(taskId == 0 ? R.string.new_task : R.string.edit_task);

        titleLayout = findViewById(R.id.title_layout);
        titleInput = findViewById(R.id.input_title);
        noteInput = findViewById(R.id.input_note);
        categoryInput = findViewById(R.id.input_category);
        dueButton = findViewById(R.id.button_due);
        clearDueButton = findViewById(R.id.button_clear_due);
        reminderSwitch = findViewById(R.id.switch_reminder);

        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        String[] categories = getResources().getStringArray(R.array.categories);
        categoryInput.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_dropdown, categories));

        dueButton.setOnClickListener(view -> showDatePicker());
        findViewById(R.id.button_save).setOnClickListener(view -> save());
        clearDueButton.setOnClickListener(view -> {
            if (task != null) {
                task.dueAt = 0;
                showDue();
            }
        });
        reminderSwitch.setEnabled(false);
        // Check the system notification toggle before enabling a reminder.
        reminderSwitch.setOnClickListener(view -> {
            if (reminderSwitch.isChecked() && !NotificationManagerCompat
                    .from(this).areNotificationsEnabled()) {
                Snackbar.make(findViewById(R.id.edit_root),
                                R.string.reminder_permission, Snackbar.LENGTH_LONG)
                        .setAction(R.string.go_settings, v -> openNotificationSettings())
                        .show();
            }
        });

        if (taskId == 0) {
            task = new Task();
            task.createdAt = System.currentTimeMillis();
            task.category = getString(R.string.category_other);
            categoryInput.setText(task.category, false);
            loaded = true;
            showDue();
            applyRestoredState();
        } else {
            viewModel.getTask(taskId).observe(this, loadedTask -> {
                if (loaded) {
                    return;
                }
                if (loadedTask == null) {
                    finish();
                    return;
                }
                task = loadedTask.copy();
                loaded = true;
                // On restore the EditTexts repopulate themselves, so don't overwrite user input.
                if (!restoring) {
                    titleInput.setText(task.title);
                    noteInput.setText(task.note);
                    categoryInput.setText(task.category, false);
                    reminderSwitch.setChecked(task.reminder);
                }
                showDue();
                applyRestoredState();
            });
        }
    }

    /**
     * Saves the unsaved due time and reminder switch so they survive recreation.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (task != null) {
            outState.putLong(STATE_DUE_AT, task.dueAt);
            outState.putBoolean(STATE_REMINDER, reminderSwitch.isChecked());
        }
    }

    /**
     * Stores the restored values and applies them once the task is available.
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoredDueAt = savedInstanceState.getLong(STATE_DUE_AT, -1);
        restoredReminder = savedInstanceState.getBoolean(STATE_REMINDER, false);
        applyRestoredState();
    }

    /**
     * Applies the restored due time and reminder, then clears them so later input isn't overwritten.
     */
    private void applyRestoredState() {
        if (restoredDueAt < 0 || task == null) {
            return;
        }
        task.dueAt = restoredDueAt;
        showDue();
        if (reminderSwitch.isEnabled()) {
            reminderSwitch.setChecked(restoredReminder);
        }
        restoredDueAt = -1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        menu.findItem(R.id.action_delete).setVisible(taskId != 0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Validates the form and saves the task, guarding against duplicate clicks.
     */
    private void save() {
        if (task == null || saving) {
            return;
        }
        String title = textOf(titleInput);
        if (title.isEmpty()) {
            titleLayout.setError(getString(R.string.title_required));
            titleInput.requestFocus();
            return;
        }
        titleLayout.setError(null);
        saving = true;

        String category = textOf(categoryInput);
        if (category.isEmpty()) {
            category = getString(R.string.category_other);
        }
        if (!isKnownCategory(category)) {
            category = getString(R.string.category_other);
            categoryInput.setText(category, false);
        }

        task.title = title;
        task.note = textOf(noteInput);
        task.category = category;
        task.reminder = reminderSwitch.isChecked() && task.dueAt > 0;

        viewModel.save(task, id -> finish());
    }

    /** Asks for confirmation, then deletes the task and closes the screen. */
    private void confirmDelete() {
        if (task == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.delete(task);
                    finish();
                })
                .show();
    }

    /** Shows the date picker, defaulting to the current due time or one hour from now. */
    private void showDatePicker() {
        LocalDateTime initial = task != null && task.dueAt > 0
                ? toDateTime(task.dueAt)
                : LocalDateTime.now().plusHours(1).withSecond(0).withNano(0);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) ->
                showTimePicker(year, month, dayOfMonth),
                initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        dialog.show();
    }

    /**
     * Shows the time picker and combines the chosen date and time into the due timestamp.
     */
    private void showTimePicker(int year, int month, int dayOfMonth) {
        LocalDateTime initial = task != null && task.dueAt > 0
                ? toDateTime(task.dueAt)
                : LocalDateTime.now().plusHours(1);

        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            if (task == null) {
                return;
            }
            LocalDateTime dateTime = LocalDateTime.of(year, month + 1, dayOfMonth,
                    hourOfDay, minute);
            task.dueAt = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            showDue();
        }, initial.getHour(), initial.getMinute(), true);
        dialog.show();
    }

    /**
     * Updates the due date label and enables the reminder switch only for future due times.
     */
    private void showDue() {
        boolean hasDue = task != null && task.dueAt > 0;
        dueButton.setText(hasDue ? DateFormats.formatFull(task.dueAt) : getString(R.string.due_pick));
        clearDueButton.setVisibility(hasDue ? View.VISIBLE : View.GONE);
        boolean canRemind = hasDue && task.dueAt > System.currentTimeMillis();
        reminderSwitch.setEnabled(canRemind);
        if (!canRemind) {
            reminderSwitch.setChecked(false);
        }
    }

    /** Returns the trimmed text, or an empty string when there is none. */
    private static String textOf(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    /** Returns true when the category is one of the predefined entries. */
    private boolean isKnownCategory(String category) {
        for (String known : getResources().getStringArray(R.array.categories)) {
            if (known.equals(category)) {
                return true;
            }
        }
        return false;
    }

    /** Converts a millisecond timestamp to a local date-time. */
    private static LocalDateTime toDateTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        try {
            startActivity(intent);
        } catch (Exception ignored) {
            // Some devices do not provide this settings page.
        }
    }
}
