package com.example.pomodoro.view;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pomodoro.R;
import com.example.pomodoro.model.TaskStatDTO;
import com.example.pomodoro.viewmodel.TaskViewModel;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {
    private TaskViewModel taskViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        Toolbar toolbar = findViewById(R.id.toolbar_statistics);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Thống kê theo ngày");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_statistics);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        StatAdapter adapter = new StatAdapter();
        recyclerView.setAdapter(adapter);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskViewModel.getLast30DaysStats().observe(this, adapter::setStats);

        adapter.setOnItemClickListener(date -> {
            taskViewModel.getTasksForDate(date).observe(StatisticsActivity.this, new androidx.lifecycle.Observer<List<TaskStatDTO>>() {
                @Override
                public void onChanged(List<TaskStatDTO> taskStats) {
                    taskViewModel.getTasksForDate(date).removeObserver(this);
                    showTasksDialog(date, taskStats);
                }
            });
        });
    }

    private void showTasksDialog(String date, List<TaskStatDTO> taskStats) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_bottom_sheet_tasks, null);
        bottomSheetDialog.setContentView(view);

        TextView textTitle = view.findViewById(R.id.text_dialog_title);
        textTitle.setText("Chi tiết công việc ngày " + date);

        LinearLayout container = view.findViewById(R.id.layout_tasks_container);

        if (taskStats == null || taskStats.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Không có dữ liệu công việc.");
            emptyText.setPadding(0, 16, 0, 16);
            container.addView(emptyText);
        } else {
            for (TaskStatDTO t : taskStats) {
                View item = getLayoutInflater().inflate(R.layout.item_task_detail, container, false);

                TextView textName = item.findViewById(R.id.text_detail_task_name);
                TextView textPomos = item.findViewById(R.id.text_detail_pomodoros);
                TextView textMins = item.findViewById(R.id.text_detail_minutes);

                textName.setText(t.taskName);
                textPomos.setText("🍅 " + t.pomodoros + " phiên");
                textMins.setText("⏱ " + t.totalMinutes + " phút");

                container.addView(item);
            }
        }

        bottomSheetDialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}