package com.example.pomodoro.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.pomodoro.model.DailyStat;
import com.example.pomodoro.model.Task;
import com.example.pomodoro.model.TaskSession;


@Database(entities = {Task.class, DailyStat.class, TaskSession.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract TaskDao taskDao();
    public abstract DailyStatDao dailyStatDao();
    public abstract TaskSessionDao taskSessionDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "pomodoro_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}