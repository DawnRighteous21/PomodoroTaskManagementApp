package com.example.pomodoro;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pomodoro.model.Task;
import com.example.pomodoro.view.TaskAdapter;
import com.example.pomodoro.view.TimerActivity;
import com.example.pomodoro.viewmodel.TaskViewModel;
import com.example.pomodoro.service.TimerService; // Import required

public class MainActivity extends AppCompatActivity {
    private TaskViewModel taskViewModel;
    private SwitchCompat switchStrictMode;
    private RecyclerView recyclerView;
    private static boolean isFreshLaunch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_PHONE_STATE}, 101);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 101);
            }
        }

        switchStrictMode = findViewById(R.id.switch_strict_mode);
        switchStrictMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && buttonView.isPressed()) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Enable Strict Mode?")
                        .setMessage("In Strict Mode, you cannot pause the timer. Leaving the app for more than 5 seconds will reset your progress. Are you sure?")
                        .setPositiveButton("Enable", null)
                        .setNegativeButton("Cancel", (dialog, which) -> switchStrictMode.setChecked(false))
                        .setCancelable(false)
                        .show();
            }
        });

        recyclerView = findViewById(R.id.recycler_view_tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        TaskAdapter adapter = new TaskAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new TaskAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Task task) {
                int runningId = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE).getInt("activeTaskId", -1);
                if (runningId != -1 && runningId != task.getId()) {
                    Toast.makeText(MainActivity.this, "Finish the current active task first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (task.getCompletedPomodoros() >= task.getEstimatedPomodoros()) {
                    Toast.makeText(MainActivity.this, "Task already completed!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(MainActivity.this, TimerActivity.class);
                intent.putExtra("EXTRA_TASK_NAME", task.getName());
                intent.putExtra("EXTRA_TASK_ID", task.getId());
                intent.putExtra("EXTRA_TASK_COMPLETED", task.getCompletedPomodoros());
                intent.putExtra("EXTRA_TASK_ESTIMATED", task.getEstimatedPomodoros());
                intent.putExtra("EXTRA_STRICT_MODE", switchStrictMode.isChecked());
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Task task) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Task")
                        .setMessage("Delete '" + task.getName() + "'?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // DETECT IF DELETING ACTIVE TASK
                            int runningId = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE).getInt("activeTaskId", -1);
                            if (runningId == task.getId()) {
                                Intent stopIntent = new Intent(MainActivity.this, TimerService.class);
                                stopIntent.setAction(TimerService.ACTION_FORCE_STOP);
                                startService(stopIntent);

                                // Clear visual indicator instantly
                                if (recyclerView.getAdapter() instanceof TaskAdapter) {
                                    ((TaskAdapter) recyclerView.getAdapter()).setActiveTaskId(-1);
                                }
                            }
                            taskViewModel.delete(task);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskViewModel.getAllTasks().observe(this, adapter::setTasks);

        findViewById(R.id.fab_add_task).setOnClickListener(v -> showAddTaskDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE);
        int activeTaskId = prefs.getInt("activeTaskId", -1);

        if (isFreshLaunch && activeTaskId != -1) {
            isFreshLaunch = false;
            Intent intent = new Intent(this, TimerActivity.class);
            intent.putExtra("EXTRA_TASK_ID", activeTaskId);
            intent.putExtra("EXTRA_TASK_NAME", prefs.getString("activeTaskName", ""));
            intent.putExtra("EXTRA_TASK_COMPLETED", prefs.getInt("activeTaskCompleted", 0));
            intent.putExtra("EXTRA_TASK_ESTIMATED", prefs.getInt("activeTaskEstimated", 1));
            intent.putExtra("EXTRA_STRICT_MODE", prefs.getBoolean("activeTaskStrict", false));
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }
        isFreshLaunch = false;

        if (recyclerView != null && recyclerView.getAdapter() instanceof TaskAdapter) {
            ((TaskAdapter) recyclerView.getAdapter()).setActiveTaskId(activeTaskId);
        }
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm công việc mới");

        View view = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        EditText editTaskName = view.findViewById(R.id.edit_task_name);
        EditText editPomodoros = view.findViewById(R.id.edit_estimated_pomodoros);

        builder.setView(view);
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String name = editTaskName.getText().toString().trim();
            String pomodoroStr = editPomodoros.getText().toString().trim();

            if (!name.isEmpty() && !pomodoroStr.isEmpty()) {
                int pomodoros = Integer.parseInt(pomodoroStr);
                taskViewModel.insert(new Task(name, pomodoros));
            } else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}
