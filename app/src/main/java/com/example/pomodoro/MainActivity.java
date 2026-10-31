package com.example.pomodoro;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pomodoro.model.Task;
import com.example.pomodoro.view.TaskAdapter;
import com.example.pomodoro.viewmodel.TaskViewModel;
import android.content.Intent;
import com.example.pomodoro.view.TimerActivity;

public class MainActivity extends AppCompatActivity {
    private TaskViewModel taskViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recycler_view_tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        TaskAdapter adapter = new TaskAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(task -> {
            Intent intent = new Intent(MainActivity.this, TimerActivity.class);
            intent.putExtra("EXTRA_TASK_NAME", task.getName());
            startActivity(intent);
        });

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskViewModel.getAllTasks().observe(this, adapter::setTasks);

        findViewById(R.id.fab_add_task).setOnClickListener(v -> showAddTaskDialog());
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