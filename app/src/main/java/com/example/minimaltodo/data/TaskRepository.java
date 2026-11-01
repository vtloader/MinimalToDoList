package com.example.minimaltodo.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single entry point for task data. Runs every database call on a background
 * thread and delivers results back on the main thread.
 */
public class TaskRepository {

    /** Receives an asynchronous result on the main thread. */
    public interface Callback<T> {
        /** Delivers the result on the main thread. */
        void onResult(T value);
    }

    private static volatile TaskRepository instance;

    private final TaskDao dao;
    // Single thread keeps database writes in order.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // Posts background results back to the main thread.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TaskRepository(Context context) {
        dao = TaskDatabase.getInstance(context).taskDao();
    }

    /** Returns the repository singleton. */
    public static TaskRepository get(Context context) {
        if (instance == null) {
            synchronized (TaskRepository.class) {
                if (instance == null) {
                    instance = new TaskRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /** Observes all tasks, already sorted for display. */
    public LiveData<List<Task>> observeTasks() {
        return dao.observeAll();
    }

    /** Observes a single task; emits null when it no longer exists. */
    public LiveData<Task> observeTask(long id) {
        return dao.observeById(id);
    }

    /** Inserts a task on the background thread. */
    public void insert(Task task) {
        executor.execute(() -> dao.insert(task));
    }

    /** Inserts a task and delivers the new id on the main thread. */
    public void insert(Task task, Callback<Long> callback) {
        executor.execute(() -> {
            long id = dao.insert(task);
            mainHandler.post(() -> callback.onResult(id));
        });
    }

    /** Updates a task on the background thread. */
    public void update(Task task) {
        executor.execute(() -> dao.update(task));
    }

    /** Deletes a task on the background thread. */
    public void delete(Task task) {
        executor.execute(() -> dao.delete(task));
    }

    /** Deletes all completed tasks on the background thread. */
    public void deleteCompleted() {
        executor.execute(dao::deleteCompleted);
    }

    /** Loads all tasks and delivers them on the main thread. */
    public void getAll(Callback<List<Task>> callback) {
        executor.execute(() -> {
            List<Task> tasks = dao.getAll();
            mainHandler.post(() -> callback.onResult(tasks));
        });
    }

    /** Loads tasks still due for a reminder and delivers them on the main thread. */
    public void getPendingReminders(Callback<List<Task>> callback) {
        executor.execute(() -> {
            List<Task> tasks = dao.getPendingReminders(System.currentTimeMillis());
            mainHandler.post(() -> callback.onResult(tasks));
        });
    }
}
