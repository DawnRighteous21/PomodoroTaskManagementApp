package com.example.pomodoro.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pomodoro.R;
import com.example.pomodoro.model.Task;
import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> tasks = new ArrayList<>();
    private OnItemClickListener listener;
    private int activeTaskId = -1;

    public interface OnItemClickListener {
        void onItemClick(Task task);
        void onItemLongClick(Task task);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setActiveTaskId(int id) {
        this.activeTaskId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task currentTask = tasks.get(position);

        String countText = currentTask.getCompletedPomodoros() + "/" + currentTask.getEstimatedPomodoros();
        holder.pomodoroCount.setText(countText);
        holder.checkBox.setChecked(currentTask.isCompleted());

        if (currentTask.getId() == activeTaskId) {
            holder.itemView.setBackgroundColor(android.graphics.Color.parseColor("#E0F7FA"));
            holder.taskName.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.taskName.setText(currentTask.getName() + " (Active)");
        } else {
            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            holder.taskName.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.taskName.setText(currentTask.getName());
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentTask);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(currentTask);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private TextView taskName;
        private TextView pomodoroCount;
        private CheckBox checkBox;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.text_task_name);
            pomodoroCount = itemView.findViewById(R.id.text_pomodoro_count);
            checkBox = itemView.findViewById(R.id.checkbox_task);
        }
    }
}
