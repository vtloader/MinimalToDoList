package com.example.minimaltodo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minimaltodo.data.Task;
import com.example.minimaltodo.util.DateFormats;

import java.util.Objects;

/**
 * Renders tasks into list rows and forwards clicks and checkbox changes to the listener.
 */
public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    /** Receives list events and forwards them to the view model. */
    public interface Listener {
        void onTaskClick(Task task);

        void onTaskChecked(Task task, boolean checked);
    }

    private final Listener listener;

    public TaskAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    /**
     * Binds a task to a recycled holder.
     */
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = getItem(position);
        if (task == null) {
            return;
        }

        holder.title.setText(task.title);
        // Completed titles are dimmed and struck through.
        // Clear the strike flag with & ~FLAG so other paint flags are preserved.
        holder.title.setAlpha(task.completed ? 0.5f : 1f);
        holder.title.setPaintFlags(task.completed
                ? holder.title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                : holder.title.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

        if (task.note.isEmpty()) {
            holder.note.setVisibility(View.GONE);
        } else {
            holder.note.setVisibility(View.VISIBLE);
            holder.note.setText(task.note);
            holder.note.setAlpha(task.completed ? 0.5f : 1f);
        }

        if (task.dueAt > 0) {
            holder.due.setVisibility(View.VISIBLE);
            // Use the warning color only for active tasks past their due date.
            boolean overdue = !task.completed && task.dueAt < System.currentTimeMillis();
            String label = DateFormats.formatDue(task.dueAt);
            holder.due.setText(overdue
                    ? holder.itemView.getContext().getString(R.string.overdue_due, label)
                    : label);
            holder.due.setTextColor(overdue
                    ? ContextCompat.getColor(holder.itemView.getContext(), R.color.overdue)
                    : holder.defaultDueColor);
            holder.due.setAlpha(task.completed ? 0.5f : 1f);
        } else {
            holder.due.setVisibility(View.GONE);
        }

        // Detach the listener before setChecked so recycled rows don't fire a stale callback.
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(task.completed);
        holder.checkBox.setContentDescription(task.title);
        holder.checkBox.setOnCheckedChangeListener(
                (buttonView, checked) -> listener.onTaskChecked(task, checked));

        holder.itemView.setOnClickListener(view -> listener.onTaskClick(task));
    }

    /** Returns the task at the given position, or null when out of range. */
    @Nullable
    public Task taskAt(int position) {
        if (position < 0 || position >= getItemCount()) {
            return null;
        }
        return super.getItem(position);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final TextView title;
        final TextView note;
        final TextView due;
        final int defaultDueColor;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox);
            title = itemView.findViewById(R.id.text_title);
            note = itemView.findViewById(R.id.text_note);
            due = itemView.findViewById(R.id.text_due);
            defaultDueColor = due.getCurrentTextColor();
        }
    }

    /**
     * Tells DiffUtil whether rows are the same item and whether their contents changed.
     */
    private static final DiffUtil.ItemCallback<Task> DIFF = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.completed == newItem.completed
                    && oldItem.reminder == newItem.reminder
                    && oldItem.dueAt == newItem.dueAt
                    && Objects.equals(oldItem.title, newItem.title)
                    && Objects.equals(oldItem.note, newItem.note)
                    && Objects.equals(oldItem.category, newItem.category);
        }
    };
}
