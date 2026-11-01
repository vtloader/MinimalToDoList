package com.example.minimaltodo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.minimaltodo.util.Prefs;

/**
 * Hosts the task list fragment and provides search and settings entry points.
 */
public class MainActivity extends AppCompatActivity {

    private TaskViewModel viewModel;

    /**
     * Sets up the toolbar and shared view model, adds the list fragment on first creation,
     * and reschedules pending reminders.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TaskListFragment())
                    .commit();
        }

        viewModel.refreshReminders();
        askForNotificationPermissionOnce();    }

    /**
     * Builds the toolbar menu and keeps the search field in sync with the view model.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    viewModel.setQuery(newText);
                    return true;
                }
            });
            String savedQuery = viewModel.getQuery();
            if (!savedQuery.isEmpty()) {
                searchItem.expandActionView();
                searchView.setQuery(savedQuery, false);
            }
        }

        menu.findItem(R.id.action_settings).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        });
        return true;
    }

    /**
     * Requests the notification permission once on Android 13 and above.
     */
    private void askForNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (Prefs.wasNotificationAsked(this)) {
            return;
        }
        Prefs.markNotificationAsked(this);
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
    }
}
