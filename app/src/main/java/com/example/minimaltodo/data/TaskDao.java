package com.example.minimaltodo.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Room data access object for the {@code tasks} table. Queries that return
 * {@link LiveData} are observed by Room; the rest run on the calling thread.
 */
@Dao
public interface TaskDao {

    /** Observes all tasks sorted for display: unfinished first, then by due date. */
    @Query("SELECT * FROM tasks " +
            "ORDER BY completed ASC, CASE WHEN dueAt = 0 THEN 1 ELSE 0 END, dueAt ASC, createdAt DESC")
    LiveData<List<Task>> observeAll();

    /** Observes a single task; emits null when the id no longer exists. */
    @Query("SELECT * FROM tasks WHERE id = :id")
    LiveData<Task> observeById(long id);

    /** Returns all tasks synchronously; used for one-off lookups such as reminders. */
    @Query("SELECT * FROM tasks")
    List<Task> getAll();

    /** Returns unfinished tasks whose reminder is enabled and due after {@code now}. */
    @Query("SELECT * FROM tasks WHERE reminder = 1 AND completed = 0 AND dueAt > :now")
    List<Task> getPendingReminders(long now);

    /** Inserts a task and returns its new row id, replacing any row with the same id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Task task);

    /** Updates the row with the same primary key; does nothing if it is missing. */
    @Update
    void update(Task task);

    /** Deletes the row with the same primary key. */
    @Delete
    void delete(Task task);

    /** Deletes every completed task. */
    @Query("DELETE FROM tasks WHERE completed = 1")
    void deleteCompleted();
}
