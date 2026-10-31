package com.example.pomodoro.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.pomodoro.model.Task;

@Database(entities = {Task.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract TaskDao taskDao();

    // Singleton instance
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