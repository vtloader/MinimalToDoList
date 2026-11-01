package com.example.minimaltodo.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Entry point to the Room database. Declares the {@code tasks} table and
 * exposes the task DAO through a process-wide singleton.
 */
@Database(entities = {Task.class}, version = 1, exportSchema = false)
public abstract class TaskDatabase extends RoomDatabase {

    private static volatile TaskDatabase instance;

    /** Returns the DAO for the {@code tasks} table. */
    public abstract TaskDao taskDao();

    /** Returns the singleton database instance. */
    public static TaskDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (TaskDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            TaskDatabase.class,
                            "todo.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
