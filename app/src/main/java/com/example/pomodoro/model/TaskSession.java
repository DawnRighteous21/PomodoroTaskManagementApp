package com.example.pomodoro.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "task_sessions")
public class TaskSession {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String date;
    private String taskName;
    private int workMinutes;

    public TaskSession(String date, String taskName, int workMinutes) {
        this.date = date;
        this.taskName = taskName;
        this.workMinutes = workMinutes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDate() { return date; }
    public String getTaskName() { return taskName; }
    public int getWorkMinutes() { return workMinutes; }
}