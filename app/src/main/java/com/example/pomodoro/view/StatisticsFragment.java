package com.example.pomodoro.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pomodoro.R;
import com.example.pomodoro.model.TaskStatDTO;
import com.example.pomodoro.viewmodel.TaskViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.List;

public class StatisticsFragment extends Fragment {

    private TaskViewModel taskViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_statistics);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        StatAdapter adapter = new StatAdapter();
        recyclerView.setAdapter(adapter);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        taskViewModel.getLast30DaysStats().observe(getViewLifecycleOwner(), adapter::setStats);

        adapter.setOnItemClickListener(date -> {
            taskViewModel.getTasksForDate(date).observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<List<TaskStatDTO>>() {
                @Override
                public void onChanged(List<TaskStatDTO> taskStats) {
                    taskViewModel.getTasksForDate(date).removeObserver(this);
                    showTasksDialog(date, taskStats);
                }
            });
        });

        return view;
    }

    private void showTasksDialog(String date, List<TaskStatDTO> taskStats) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_bottom_sheet_tasks, null);
        bottomSheetDialog.setContentView(view);

        TextView textTitle = view.findViewById(R.id.text_dialog_title);
        textTitle.setText("Chi tiết công việc ngày " + date);

        LinearLayout container = view.findViewById(R.id.layout_tasks_container);

        if (taskStats == null || taskStats.isEmpty()) {
            TextView emptyText = new TextView(requireContext());
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
}