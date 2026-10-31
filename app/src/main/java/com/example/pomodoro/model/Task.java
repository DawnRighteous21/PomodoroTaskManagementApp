package com.example.pomodoro.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private int estimatedPomodoros;
    private int completedPomodoros;
    private boolean isCompleted;

    public Task(String name, int estimatedPomodoros) {
        this.name = name;
        this.estimatedPomodoros = estimatedPomodoros;
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
    public void setCompleted(boolean completed) { isCompleted = completed; }
}