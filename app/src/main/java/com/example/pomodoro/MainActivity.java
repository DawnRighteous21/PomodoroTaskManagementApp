package com.example.pomodoro;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import com.example.pomodoro.model.Task;
import com.example.pomodoro.view.StatisticsActivity;
import com.example.pomodoro.view.TaskAdapter;
import com.example.pomodoro.view.TimerActivity;
import com.example.pomodoro.viewmodel.TaskViewModel;
import com.example.pomodoro.service.TimerService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TaskViewModel taskViewModel;
    private SwitchCompat switchStrictMode;
    private RecyclerView recyclerView;
    private TextView textDevIndicator;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_statistics) {
                startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });

        textDevIndicator = findViewById(R.id.text_dev_indicator);
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
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Chuyển công việc?")
                            .setMessage("Một công việc khác đang chạy. Bạn có muốn dừng nó và chuyển sang công việc này không?")
                            .setPositiveButton("Chuyển", (dialog, which) -> {
                                Intent stopIntent = new Intent(MainActivity.this, TimerService.class);
                                stopIntent.setAction(TimerService.ACTION_FORCE_STOP);
                                startService(stopIntent);
                                launchTimer(task);
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                    return;
                }

                if (task.isCompleted()) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Task Completed")
                            .setMessage("Do you want to restart this task from session 0?")
                            .setPositiveButton("Restart", (dialog, which) -> {
                                task.setCompletedPomodoros(0);
                                task.setCompleted(false);
                                taskViewModel.update(task);
                                launchTimer(task);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    return;
                }

                launchTimer(task);
            }

            @Override
            public void onItemLongClick(Task task) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Tùy chọn")
                        .setPositiveButton("Sửa", (dialog, which) -> showTaskDialog(task))
                        .setNegativeButton("Xóa", (dialog, which) -> {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Xác nhận")
                                    .setMessage("Bạn có chắc chắn muốn xóa công việc này?")
                                    .setPositiveButton("Xóa", (confirmDialog, confirmWhich) -> {
                                        int runningId = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE).getInt("activeTaskId", -1);
                                        if (runningId == task.getId()) {
                                            Intent stopIntent = new Intent(MainActivity.this, TimerService.class);
                                            stopIntent.setAction(TimerService.ACTION_FORCE_STOP);
                                            startService(stopIntent);
                                            if (recyclerView.getAdapter() instanceof TaskAdapter) {
                                                ((TaskAdapter) recyclerView.getAdapter()).setActiveTaskId(-1);
                                            }
                                        }
                                        taskViewModel.delete(task);
                                    })
                                    .setNegativeButton("Hủy", null)
                                    .show();
                        })
                        .setNeutralButton("Hủy", null)
                        .show();
            }
        });

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskViewModel.getAllTasks().observe(this, adapter::setTasks);

        FloatingActionButton fab = findViewById(R.id.fab_add_task);
        fab.setOnClickListener(v -> showTaskDialog(null));

        fab.setOnLongClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("TimerPrefs", MODE_PRIVATE);
            boolean isDev = prefs.getBoolean("dev_mode", false);
            prefs.edit().putBoolean("dev_mode", !isDev).apply();
            updateDevModeUI(!isDev);
            return true;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateDevModeUI(boolean isDev) {
        textDevIndicator.setVisibility(isDev ? View.VISIBLE : View.GONE);
    }

    private void launchTimer(Task task) {
        Intent intent = new Intent(MainActivity.this, TimerActivity.class);
        intent.putExtra("EXTRA_TASK", task);
        intent.putExtra("EXTRA_STRICT_MODE", switchStrictMode.isChecked());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE);

        updateDevModeUI(prefs.getBoolean("dev_mode", false));

        if (!TimerService.isServiceRunning) {
            prefs.edit().putInt("activeTaskId", -1).apply();
        }

        int activeTaskId = prefs.getInt("activeTaskId", -1);

        if (recyclerView != null && recyclerView.getAdapter() instanceof TaskAdapter) {
            ((TaskAdapter) recyclerView.getAdapter()).setActiveTaskId(activeTaskId);
        }
    }

    private void showTaskDialog(Task existingTask) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existingTask == null ? "Thêm công việc mới" : "Sửa công việc");

        View view = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        EditText editTaskName = view.findViewById(R.id.edit_task_name);
        EditText editPomodoros = view.findViewById(R.id.edit_estimated_pomodoros);
        EditText editWorkTime = view.findViewById(R.id.edit_work_time);
        EditText editBreakTime = view.findViewById(R.id.edit_break_time);
        TextView textWorkLabel = view.findViewById(R.id.text_work_label);
        TextView textBreakLabel = view.findViewById(R.id.text_break_label);

        boolean isDevTaskMode;
        if (existingTask != null) {
            isDevTaskMode = existingTask.isDevTask();
        } else {
            isDevTaskMode = getSharedPreferences("TimerPrefs", MODE_PRIVATE).getBoolean("dev_mode", false);
        }

        if (isDevTaskMode) {
            textWorkLabel.setText("Làm việc (giây)");
            textBreakLabel.setText("Nghỉ ngơi (giây)");
        }

        if (existingTask != null) {
            editTaskName.setText(existingTask.getName());
            editPomodoros.setText(String.valueOf(existingTask.getEstimatedPomodoros()));
            editWorkTime.setText(String.valueOf(existingTask.getWorkDuration()));
            editBreakTime.setText(String.valueOf(existingTask.getBreakDuration()));
        }

        builder.setView(view);
        builder.setPositiveButton("Lưu", null);
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = editTaskName.getText().toString().trim();
            String pomodoroStr = editPomodoros.getText().toString().trim();
            String workStr = editWorkTime.getText().toString().trim();
            String breakStr = editBreakTime.getText().toString().trim();

            if (name.isEmpty() || pomodoroStr.isEmpty() || workStr.isEmpty() || breakStr.isEmpty()) {
                new AlertDialog.Builder(MainActivity.this).setTitle("Lỗi").setMessage("Vui lòng nhập đầy đủ thông tin").setPositiveButton("OK", null).show();
                return;
            }

            List<Task> currentTasks = ((TaskAdapter) recyclerView.getAdapter()).getTasks();
            for (Task t : currentTasks) {
                if (t.getName().equalsIgnoreCase(name) && (existingTask == null || existingTask.getId() != t.getId())) {
                    new AlertDialog.Builder(MainActivity.this).setTitle("Lỗi").setMessage("Tên công việc này đã tồn tại!").setPositiveButton("OK", null).show();
                    return;
                }
            }

            int pomodoros = Integer.parseInt(pomodoroStr);
            int workMins = Integer.parseInt(workStr);
            int breakMins = Integer.parseInt(breakStr);

            if (workMins < 1 || breakMins < 1 || pomodoros < 1) {
                new AlertDialog.Builder(MainActivity.this).setTitle("Lỗi").setMessage("Giá trị phải lớn hơn 0").setPositiveButton("OK", null).show();
                return;
            }

            if (!isDevTaskMode) {
                if (workMins < 10 || workMins > 90) {
                    new AlertDialog.Builder(MainActivity.this).setTitle("Lỗi").setMessage("Thời gian làm việc phải từ 10 - 90 phút").setPositiveButton("OK", null).show();
                    return;
                }
                if (breakMins < 1 || breakMins > 30) {
                    new AlertDialog.Builder(MainActivity.this).setTitle("Lỗi").setMessage("Thời gian nghỉ phải từ 1 - 30 phút").setPositiveButton("OK", null).show();
                    return;
                }
            }

            if (existingTask == null) {
                taskViewModel.insert(new Task(name, pomodoros, workMins, breakMins, isDevTaskMode));
            } else {
                existingTask.setName(name);
                existingTask.setEstimatedPomodoros(pomodoros);
                existingTask.setWorkDuration(workMins);
                existingTask.setBreakDuration(breakMins);
                taskViewModel.update(existingTask);

                int runningId = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE).getInt("activeTaskId", -1);
                if (runningId == existingTask.getId()) {
                    Intent stopIntent = new Intent(MainActivity.this, TimerService.class);
                    stopIntent.setAction(TimerService.ACTION_FORCE_STOP);
                    startService(stopIntent);
                    new AlertDialog.Builder(MainActivity.this).setTitle("Thông báo").setMessage("Công việc đang chạy đã được cập nhật và dừng lại.").setPositiveButton("OK", null).show();
                }
            }
            dialog.dismiss();
        });
    }
}