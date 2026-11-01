package com.example.pomodoro.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "tasks")
public class Task implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private int estimatedPomodoros;
    private int completedPomodoros;
    private boolean isCompleted;
    private int workDuration;
    private int breakDuration;
    private boolean isDevTask;

    public Task(String name, int estimatedPomodoros, int workDuration, int breakDuration, boolean isDevTask) {
        this.name = name;
        this.estimatedPomodoros = estimatedPomodoros;
        this.workDuration = workDuration;
        this.breakDuration = breakDuration;
        this.isDevTask = isDevTask;
        this.completedPomodoros = 0;
        this.isCompleted = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getEstimatedPomodoros() { return estimatedPomodoros; }
    public void setEstimatedPomodoros(int estimatedPomodoros) { this.estimatedPomodoros = estimatedPomodoros; }
    public int getCompletedPomodoros() { return completedPomodoros; }
    public void setCompletedPomodoros(int completedPomodoros) { this.completedPomodoros = completedPomodoros; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }
    public int getWorkDuration() { return workDuration; }
    public void setWorkDuration(int workDuration) { this.workDuration = workDuration; }
    public int getBreakDuration() { return breakDuration; }
    public void setBreakDuration(int breakDuration) { this.breakDuration = breakDuration; }
    public boolean isDevTask() { return isDevTask; }
    public void setDevTask(boolean devTask) { isDevTask = devTask; }
}