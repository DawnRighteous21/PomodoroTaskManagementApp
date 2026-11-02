package com.example.pomodoro.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.pomodoro.model.TaskSession;
import com.example.pomodoro.model.TaskStatDTO;
import java.util.List;

@Dao
public interface TaskSessionDao {
    @Insert
    void insert(TaskSession session);

    @Query("SELECT taskName, COUNT(id) as pomodoros, SUM(workMinutes) as totalMinutes " +
            "FROM task_sessions WHERE date = :date GROUP BY taskName")
    LiveData<List<TaskStatDTO>> getTasksForDate(String date);
}