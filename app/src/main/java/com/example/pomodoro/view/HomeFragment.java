package com.example.pomodoro.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pomodoro.R;
import com.example.pomodoro.model.Task;
import com.example.pomodoro.service.TimerService;
import com.example.pomodoro.viewmodel.TaskViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class HomeFragment extends Fragment {

    private TaskViewModel taskViewModel;
    private RecyclerView recyclerView;
    private TextView textDevIndicator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        textDevIndicator = view.findViewById(R.id.text_dev_indicator);
        recyclerView = view.findViewById(R.id.recycler_view_tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        TaskAdapter adapter = new TaskAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new TaskAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Task task) {
                int runningId = requireActivity().getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE).getInt("activeTaskId", -1);
                if (runningId != -1 && runningId != task.getId()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Chuyển công việc?")
                            .setMessage("Một công việc khác đang chạy. Bạn có muốn dừng nó và chuyển sang công việc này không?")
                            .setPositiveButton("Chuyển", (dialog, which) -> {
                                Intent stopIntent = new Intent(requireContext(), TimerService.class);
                                stopIntent.setAction(TimerService.ACTION_FORCE_STOP);
                                requireActivity().startService(stopIntent);
                                launchTimer(task);
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                    return;
                }

                if (task.isCompleted()) {
                    new AlertDialog.Builder(requireContext())
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
                new AlertDialog.Builder(requireContext())
                        .setTitle("Tùy chọn")
                        .setPositiveButton("Sửa", (dialog, which) -> showTaskDialog(task))
                        .setNegativeButton("Xóa", (dialog, which) -> {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Xác nhận")
                                    .setMessage("Bạn có chắc chắn muốn xóa công việc này?")
                                    .setPositiveButton("Xóa", (confirmDialog, confirmWhich) -> {
                                        int runningId = requireActivity().getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE).getInt("activeTaskId", -1);
                                        if (runningId == task.getId()) {
                                            Intent stopIntent = new Intent(requireContext(), TimerService.class);
                                            stopIntent.setAction(TimerService.ACTION_FORCE_STOP);
                                            requireActivity().startService(stopIntent);
                                            adapter.setActiveTaskId(-1);
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

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), adapter::setTasks);

        FloatingActionButton fab = view.findViewById(R.id.fab_add_task);
        fab.setOnClickListener(v -> showTaskDialog(null));

        return view;
    }

    private void updateDevModeUI(boolean isDev) {
        if (textDevIndicator != null) {
            textDevIndicator.setVisibility(isDev ? View.VISIBLE : View.GONE);
        }
    }

    private void launchTimer(Task task) {
        boolean isStrictMode = requireActivity().getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE).getBoolean("strict_mode", false);
        Intent intent = new Intent(requireContext(), TimerActivity.class);
        intent.putExtra("EXTRA_TASK", task);
        intent.putExtra("EXTRA_STRICT_MODE", isStrictMode);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = requireActivity().getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(existingTask == null ? "Thêm công việc mới" : "Sửa công việc");

        View view = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        EditText editTaskName = view.findViewById(R.id.edit_task_name);
        EditText editPomodoros = view.findViewById(R.id.edit_estimated_pomodoros);
        EditText editWorkTime = view.findViewById(R.id.edit_work_time);
        EditText editBreakTime = view.findViewById(R.id.edit_break_time);
        TextView textWorkLabel = view.findViewById(R.id.text_work_label);
        TextView textBreakLabel = view.findViewById(R.id.text_break_label);

        boolean isDevTaskMode = existingTask != null ? existingTask.isDevTask() : requireActivity().getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE).getBoolean("dev_mode", false);

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
                new AlertDialog.Builder(requireContext()).setTitle("Lỗi").setMessage("Vui lòng nhập đầy đủ thông tin").setPositiveButton("OK", null).show();
                return;
            }

            List<Task> currentTasks = ((TaskAdapter) recyclerView.getAdapter()).getTasks();
            for (Task t : currentTasks) {
                if (t.getName().equalsIgnoreCase(name) && (existingTask == null || existingTask.getId() != t.getId())) {
                    new AlertDialog.Builder(requireContext()).setTitle("Lỗi").setMessage("Tên công việc này đã tồn tại!").setPositiveButton("OK", null).show();
                    return;
                }
            }

            int pomodoros = Integer.parseInt(pomodoroStr);
            int workMins = Integer.parseInt(workStr);
            int breakMins = Integer.parseInt(breakStr);

            if (pomodoros < 1) { new AlertDialog.Builder(requireContext()).setTitle("Lỗi").setMessage("Số phiên dự kiến phải lớn hơn 0").setPositiveButton("OK", null).show(); return; }
            if (workMins < 1) { new AlertDialog.Builder(requireContext()).setTitle("Lỗi").setMessage("Thời gian làm việc phải lớn hơn 0").setPositiveButton("OK", null).show(); return; }
            if (breakMins < 1) { new AlertDialog.Builder(requireContext()).setTitle("Lỗi").setMessage("Thời gian nghỉ phải lớn hơn 0").setPositiveButton("OK", null).show(); return; }

            if (!isDevTaskMode) {
                if (workMins < 10 || workMins > 90) { new AlertDialog.Builder(requireContext()).setTitle("Lỗi").setMessage("Thời gian làm việc phải từ 10 - 90 phút").setPositiveButton("OK", null).show(); return; }
                if (breakMins < 1 || breakMins > 30) { new AlertDialog.Builder(requireContext()).setTitle("Lỗi").setMessage("Thời gian nghỉ phải từ 1 - 30 phút").setPositiveButton("OK", null).show(); return; }
            }

            if (existingTask == null) {
                taskViewModel.insert(new Task(name, pomodoros, workMins, breakMins, isDevTaskMode));
            } else {
                existingTask.setName(name);
                existingTask.setEstimatedPomodoros(pomodoros);
                existingTask.setWorkDuration(workMins);
                existingTask.setBreakDuration(breakMins);
                taskViewModel.update(existingTask);

                int runningId = requireActivity().getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE).getInt("activeTaskId", -1);
                if (runningId == existingTask.getId()) {
                    Intent stopIntent = new Intent(requireContext(), TimerService.class);
                    stopIntent.setAction(TimerService.ACTION_FORCE_STOP);
                    requireActivity().startService(stopIntent);
                    new AlertDialog.Builder(requireContext()).setTitle("Thông báo").setMessage("Công việc đang chạy đã được cập nhật và dừng lại.").setPositiveButton("OK", null).show();
                }
            }
            dialog.dismiss();
        });
    }
}