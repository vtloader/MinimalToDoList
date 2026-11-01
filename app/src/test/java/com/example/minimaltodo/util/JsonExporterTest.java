package com.example.minimaltodo.util;

import com.example.minimaltodo.data.Task;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for JsonExporter, covering the exported structure and field mapping.
 */
public class JsonExporterTest {

    /** Verifies that every task is exported with its fields and the exportedAt timestamp. */
    @Test
    public void exportsAllTasks() throws Exception {
        Task first = new Task();
        first.id = 1;
        first.title = "写作业";
        first.note = "第三章";
        first.category = "学习";
        first.dueAt = 1_700_000_000_000L;
        first.reminder = true;

        Task second = new Task();
        second.id = 2;
        second.title = "买菜";
        second.category = "生活";
        second.completed = true;

        String json = JsonExporter.toJson(Arrays.asList(first, second));

        assertTrue(json.contains("\"title\": \"写作业\""));
        assertTrue(json.contains("\"note\": \"第三章\""));
        assertTrue(json.contains("\"category\": \"学习\""));
        assertTrue(json.contains("\"dueAt\": 1700000000000"));
        assertTrue(json.contains("\"title\": \"买菜\""));
        assertTrue(json.contains("\"exportedAt\""));
    }

    /** Verifies that an empty list still produces a valid empty tasks array. */
    @Test
    public void exportsEmptyList() throws Exception {
        String json = JsonExporter.toJson(Collections.emptyList());
        assertTrue(json.contains("\"tasks\": []"));
        assertFalse(json.contains("\"title\""));
    }
}
