package com.example.pomodoro.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.pomodoro.data.TaskRepository;
import com.example.pomodoro.model.Task;
import com.example.pomodoro.model.DailyStat;
import com.example.pomodoro.model.TaskStatDTO;
import java.util.List;

public class TaskViewModel extends AndroidViewModel {
    private TaskRepository repository;
    private LiveData<List<Task>> allTasks;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
        allTasks = repository.getAllTasks();
    }

    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }

    public void insert(Task task) {
        repository.insert(task);
    }

    public void update(Task task) {
        repository.update(task);
    }

    public void delete(Task task) {
        repository.delete(task);
    }

    public void incrementPomodoroCount(int taskId) {
        repository.incrementPomodoroCount(taskId);
    }
    public LiveData<List<DailyStat>> getLast30DaysStats() {
        return repository.getLast30DaysStats();
    }
    public LiveData<List<TaskStatDTO>> getTasksForDate(String date) {
        return repository.getTasksForDate(date);
    }
}