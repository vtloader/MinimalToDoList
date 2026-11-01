package com.example.minimaltodo.util;

import com.example.minimaltodo.data.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Serializes the task list to indented JSON for export and sharing.
 */
public final class JsonExporter {

    /** Utility class; not instantiable. */
    private JsonExporter() {
    }

    /**
     * Returns the task list as JSON; an empty list still yields a valid
     * {"exportedAt":..., "tasks":[]} document.
     */
    public static String toJson(List<Task> tasks) throws Exception {
        JSONArray array = new JSONArray();
        for (Task task : tasks) {
            JSONObject item = new JSONObject();
            item.put("title", task.title);
            item.put("note", task.note);
            item.put("category", task.category);
            item.put("dueAt", task.dueAt);
            item.put("reminder", task.reminder);
            item.put("completed", task.completed);
            item.put("createdAt", task.createdAt);
            array.put(item);
        }

        // The export time tells multiple exports apart.
        JSONObject root = new JSONObject();
        root.put("exportedAt", System.currentTimeMillis());
        root.put("tasks", array);
        // Indent by two spaces so the output is readable.
        return root.toString(2);
    }
}
