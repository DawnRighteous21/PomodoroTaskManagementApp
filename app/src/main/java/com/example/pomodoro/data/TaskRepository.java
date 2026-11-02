package com.example.pomodoro.data;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.pomodoro.model.Task;
import com.example.pomodoro.model.TaskSession;
import com.example.pomodoro.model.TaskStatDTO;
import com.example.pomodoro.model.DailyStat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {
    private TaskDao taskDao;
    private DailyStatDao dailyStatDao;
    private TaskSessionDao taskSessionDao;
    private LiveData<List<Task>> allTasks;
    private ExecutorService executorService;

    public TaskRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        taskDao = database.taskDao();
        dailyStatDao = database.dailyStatDao();
        taskSessionDao = database.taskSessionDao();
        allTasks = taskDao.getAllTasks();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }

    public void insert(Task task) {
        executorService.execute(() -> taskDao.insert(task));
    }

    public void update(Task task) {
        executorService.execute(() -> taskDao.update(task));
    }

    public void delete(Task task) {
        executorService.execute(() -> taskDao.delete(task));
    }

    public void incrementPomodoroCount(int taskId) {
        executorService.execute(() -> {
            Task task = taskDao.getTaskById(taskId);
            if (task != null && !task.isCompleted()) {
                int newCount = task.getCompletedPomodoros() + 1;
                task.setCompletedPomodoros(newCount);

                if (newCount >= task.getEstimatedPomodoros()) {
                    task.setCompleted(true);
                }
                taskDao.update(task);
            }
        });
    }
    public LiveData<List<DailyStat>> getLast30DaysStats() {
        return dailyStatDao.getLast30DaysStats();
    }

    public void logSession(String date, String taskName, int workMinutes) {
        executorService.execute(() -> {
            DailyStat stat = dailyStatDao.getStatByDate(date);
            if (stat == null) {
                stat = new DailyStat(date, 1, workMinutes);
            } else {
                stat.setTotalPomodoros(stat.getTotalPomodoros() + 1);
                stat.setTotalWorkMinutes(stat.getTotalWorkMinutes() + workMinutes);
            }
            dailyStatDao.insert(stat);

            taskSessionDao.insert(new TaskSession(date, taskName, workMinutes));
        });
    }

    public LiveData<List<TaskStatDTO>> getTasksForDate(String date) {
        return taskSessionDao.getTasksForDate(date);
    }
}