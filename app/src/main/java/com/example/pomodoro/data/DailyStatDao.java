package com.example.pomodoro.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.pomodoro.model.DailyStat;
import java.util.List;

@Dao
public interface DailyStatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailyStat stat);

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    DailyStat getStatByDate(String date);

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 30")
    LiveData<List<DailyStat>> getLast30DaysStats();
}