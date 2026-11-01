package com.example.minimaltodo;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minimaltodo.data.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

/**
 * Displays the filtered task list with category chips, swipe-to-delete, and an empty state.
 */
public class TaskListFragment extends Fragment implements TaskAdapter.Listener {

    private TaskViewModel viewModel;
    private TaskAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private View emptyContainer;
    private View emptyAddButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    /**
     * Wires up the list, category chips, buttons, and empty state, then observes the tasks.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        recyclerView = view.findViewById(R.id.recycler_view);
        emptyView = view.findViewById(R.id.empty_view);
        emptyContainer = view.findViewById(R.id.empty_container);
        emptyAddButton = view.findViewById(R.id.button_empty_add);
        adapter = new TaskAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        new ItemTouchHelper(new SwipeCallback()).attachToRecyclerView(recyclerView);

        ChipGroup chipGroup = view.findViewById(R.id.chip_group);
        // The "All" chip maps to an empty category; other chips use their label as the value.
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip == null) {
                return;
            }
            String label = chip.getText().toString();
            viewModel.setCategory(label.equals(getString(R.string.category_all)) ? "" : label);
        });
        restoreSelectedChip(chipGroup);

        view.findViewById(R.id.fab).setOnClickListener(v -> openEditor());
        emptyAddButton.setOnClickListener(v -> openEditor());

        viewModel.getVisibleTasks().observe(getViewLifecycleOwner(), tasks -> {
            adapter.submitList(tasks);
            boolean empty = tasks == null || tasks.isEmpty();
            boolean filtering = viewModel.isFiltering();
            emptyView.setText(filtering ? R.string.empty_no_match : R.string.empty_tasks);
            emptyContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
            emptyAddButton.setVisibility(empty && !filtering ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    private void openEditor() {
        startActivity(new Intent(requireContext(), EditTaskActivity.class));
    }

    /**
     * Reloads preferences that the settings screen may have changed.
     */
    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshPreferences();
    }

    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(requireContext(), EditTaskActivity.class);
        intent.putExtra(EditTaskActivity.EXTRA_TASK_ID, task.id);
        startActivity(intent);
    }

    /**
     * Marks a task completed or active and offers an undo when it leaves the visible list.
     */
    @Override
    public void onTaskChecked(Task task, boolean checked) {
        viewModel.setCompleted(task, checked);
        if (checked && !viewModel.isShowingCompleted()) {
            Snackbar.make(recyclerView, R.string.task_completed, Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.fab)
                    .setAction(R.string.undo, v -> viewModel.setCompleted(task, false))
                    .show();
        }
    }

    /**
     * Handles swipe-to-delete and offers to restore the removed task.
     */
    private class SwipeCallback extends ItemTouchHelper.SimpleCallback {

        SwipeCallback() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            Task task = adapter.taskAt(position);
            if (task == null) {
                return;
            }
            Task deleted = viewModel.delete(task);
            Snackbar.make(recyclerView, R.string.task_deleted, Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.fab)
                    .setAction(R.string.undo, v -> viewModel.restore(deleted))
                    .show();
        }

        /**
         * Draws the red swipe background and the delete icon while a row is swiped.
         */
        @Override
        public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                View item = viewHolder.itemView;
                float radius = dp(16);
                int bottom = item.getBottom() - dp(8);

                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setColor(ContextCompat.getColor(requireContext(), R.color.swipe_red));
                canvas.drawRoundRect(item.getLeft(), item.getTop(), item.getRight(),
                        bottom, radius, radius, paint);

                Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);
                if (icon != null) {
                    int size = dp(24);
                    int margin = dp(24);
                    int left = dX > 0 ? item.getLeft() + margin
                            : item.getRight() - margin - size;
                    int top = item.getTop() + (bottom - item.getTop() - size) / 2;
                    icon.setBounds(left, top, left + size, top + size);
                    icon.setTint(Color.WHITE);
                    icon.draw(canvas);
                }
            }
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    /** Converts dp to pixels so the radius and icon size scale with screen density. */
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    /** Restores the selected category chip from the view model. */
    private void restoreSelectedChip(ChipGroup chipGroup) {
        String category = viewModel.getCategory();
        int id = R.id.chip_all;
        if (category.equals(getString(R.string.category_work))) {
            id = R.id.chip_work;
        } else if (category.equals(getString(R.string.category_study))) {
            id = R.id.chip_study;
        } else if (category.equals(getString(R.string.category_life))) {
            id = R.id.chip_life;
        } else if (category.equals(getString(R.string.category_other))) {
            id = R.id.chip_other;
        }
        chipGroup.check(id);
    }
}
