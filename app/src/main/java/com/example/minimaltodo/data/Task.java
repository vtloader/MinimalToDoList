package com.example.minimaltodo.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Row entity for the {@code tasks} table: what the task is, when it is due,
 * and whether it is done. Fields stay public so Room can map them by name.
 */
@Entity(tableName = "tasks")
public class Task {

    /** Auto-generated primary key; 0 for a task that has not been inserted yet. */
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String title;

    @NonNull
    public String note;

    @NonNull
    public String category;

    // 0 means "no due date".
    public long dueAt;

    public boolean reminder;
    public boolean completed;
    public long createdAt;

    /** Empty constructor for Room; initializes the non-null strings to empty values. */
    public Task() {
        title = "";
        note = "";
        category = "";
    }

    /** Returns a field-by-field copy, so the UI can edit a draft before saving. */
    public Task copy() {
        Task copy = new Task();
        copy.id = id;
        copy.title = title;
        copy.note = note;
        copy.category = category;
        copy.dueAt = dueAt;
        copy.reminder = reminder;
        copy.completed = completed;
        copy.createdAt = createdAt;
        return copy;
    }
}
