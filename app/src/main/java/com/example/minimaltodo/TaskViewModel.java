package com.example.minimaltodo;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.minimaltodo.data.Task;
import com.example.minimaltodo.data.TaskRepository;
import com.example.minimaltodo.reminder.ReminderNotifier;
import com.example.minimaltodo.reminder.ReminderScheduler;
import com.example.minimaltodo.util.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared state for the task list, combining the database, search, and category filters.
 */
public class TaskViewModel extends AndroidViewModel {

    private final TaskRepository repository;
    private final LiveData<List<Task>> source;
    private final MediatorLiveData<List<Task>> visibleTasks = new MediatorLiveData<>();
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<String> category = new MutableLiveData<>("");
    private boolean showCompleted;

    /**
     * Combines the task list, search query, and category into the filtered list.
     */
    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = TaskRepository.get(application);
        source = repository.observeTasks();
        showCompleted = Prefs.isShowCompleted(application);

        visibleTasks.addSource(source, list -> visibleTasks.setValue(filter(list)));
        visibleTasks.addSource(query, value -> visibleTasks.setValue(filter(source.getValue())));
        visibleTasks.addSource(category, value -> visibleTasks.setValue(filter(source.getValue())));
    }

    public LiveData<List<Task>> getVisibleTasks() {
        return visibleTasks;
    }

    /** Observes a single task for the editor screen. */
    public LiveData<Task> getTask(long id) {
        return repository.observeTask(id);
    }

    /** Sets the search query and refilters the list. */
    public void setQuery(String value) {
        query.setValue(value == null ? "" : value.trim());
    }

    /** Sets the category filter; an empty string shows all categories. */
    public void setCategory(String value) {
        category.setValue(value == null ? "" : value);
    }

    public String getCategory() {
        return category.getValue() == null ? "" : category.getValue();
    }

    public String getQuery() {
        return query.getValue() == null ? "" : query.getValue();
    }

    /** Returns true when a search query or category filter is active. */
    public boolean isFiltering() {
        return !getQuery().isEmpty() || !getCategory().isEmpty();
    }

    public boolean isShowingCompleted() {
        return showCompleted;
    }

    /** Reloads the "show completed" preference and refilters if it changed. */
    public void refreshPreferences() {
        boolean latest = Prefs.isShowCompleted(getApplication());
        if (latest != showCompleted) {
            showCompleted = latest;
            visibleTasks.setValue(filter(source.getValue()));
        }
    }

    /**
     * Updates the completion state on a copy of the task and reschedules its alarm.
     */
    public void setCompleted(Task task, boolean completed) {
        Task updated = task.copy();
        updated.completed = completed;
        repository.update(updated);
        updateAlarm(updated);
    }

    /**
     * Deletes a task, cancels its reminders, and returns a copy for undo.
     */
    public Task delete(Task task) {
        ReminderScheduler.cancel(getApplication(), task.id);
        ReminderNotifier.cancel(getApplication(), task.id);
        repository.delete(task);
        return task.copy();
    }

    /** Reinserts a deleted task and restores its reminder. */
    public void restore(Task task) {
        repository.insert(task);
        updateAlarm(task);
    }

    /**
     * Inserts a new task or updates an existing one, then syncs its alarm.
     */
    public void save(Task task, TaskRepository.Callback<Long> callback) {
        if (task.id == 0) {
            repository.insert(task, id -> {
                task.id = id;
                updateAlarm(task);
                if (callback != null) {
                    callback.onResult(id);
                }
            });
        } else {
            repository.update(task);
            updateAlarm(task);
            if (callback != null) {
                callback.onResult(task.id);
            }
        }
    }

    /**
     * Cancels reminders for completed tasks, then deletes them from the database.
     */
    public void clearCompleted() {
        repository.getAll(tasks -> {
            for (Task task : tasks) {
                if (task.completed) {
                    ReminderScheduler.cancel(getApplication(), task.id);
                    ReminderNotifier.cancel(getApplication(), task.id);
                }
            }
            repository.deleteCompleted();
        });
    }

    public void getAll(TaskRepository.Callback<List<Task>> callback) {
        repository.getAll(callback);
    }

    /**
     * Reschedules alarms for all pending reminders, since alarms are lost on reboot.
     */
    public void refreshReminders() {
        repository.getPendingReminders(tasks -> {
            for (Task task : tasks) {
                ReminderScheduler.schedule(getApplication(), task);
            }
        });
    }

    /**
     * Schedules or cancels the alarm based on the task's current state.
     */
    private void updateAlarm(Task task) {
        boolean needsAlarm = task.reminder
                && !task.completed
                && task.dueAt > System.currentTimeMillis();
        if (needsAlarm) {
            ReminderScheduler.schedule(getApplication(), task);
        } else {
            ReminderScheduler.cancel(getApplication(), task.id);
            if (task.completed) {
                ReminderNotifier.cancel(getApplication(), task.id);
            }
        }
    }

    /**
     * Applies the completion, search, and category filters in that order.
     */
    private List<Task> filter(List<Task> tasks) {
        if (tasks == null) {
            return new ArrayList<>();
        }
        String text = query.getValue() == null ? "" : query.getValue().toLowerCase(Locale.getDefault());
        String selectedCategory = category.getValue() == null ? "" : category.getValue();

        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (!showCompleted && task.completed) {
                continue;
            }
            boolean matchesText = TextUtils.isEmpty(text)
                    || task.title.toLowerCase(Locale.getDefault()).contains(text)
                    || task.note.toLowerCase(Locale.getDefault()).contains(text);
            boolean matchesCategory = TextUtils.isEmpty(selectedCategory)
                    || selectedCategory.equals(task.category);
            if (matchesText && matchesCategory) {
                result.add(task);
            }
        }
        return result;
    }
}
