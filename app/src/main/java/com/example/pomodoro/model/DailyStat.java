package com.example.pomodoro.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_stats")
public class DailyStat {
    @PrimaryKey
    @NonNull
    private String date;
    private int totalPomodoros;
    private int totalWorkMinutes;

    public DailyStat(@NonNull String date, int totalPomodoros, int totalWorkMinutes) {
        this.date = date;
        this.totalPomodoros = totalPomodoros;
        this.totalWorkMinutes = totalWorkMinutes;
    }

    @NonNull
    public String getDate() { return date; }
    public void setDate(@NonNull String date) { this.date = date; }
    public int getTotalPomodoros() { return totalPomodoros; }
    public void setTotalPomodoros(int totalPomodoros) { this.totalPomodoros = totalPomodoros; }
    public int getTotalWorkMinutes() { return totalWorkMinutes; }
    public void setTotalWorkMinutes(int totalWorkMinutes) { this.totalWorkMinutes = totalWorkMinutes; }
}