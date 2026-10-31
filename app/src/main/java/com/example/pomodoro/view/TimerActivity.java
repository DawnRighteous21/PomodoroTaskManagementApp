package com.example.pomodoro.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pomodoro.R;
import com.example.pomodoro.service.TimerService;

import java.util.Locale;

public class TimerActivity extends AppCompatActivity {
    private static final long START_TIME_IN_MILLIS = 1500000;

    private TextView textCountdown;
    private TextView textActiveTaskName;
    private ProgressBar progressTimer;
    private Button btnStartPause;

    private CountDownTimer countDownTimer;
    private boolean timerRunning;
    private long timeLeftInMillis = START_TIME_IN_MILLIS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        textActiveTaskName = findViewById(R.id.text_active_task_name);
        textCountdown = findViewById(R.id.text_countdown);
        progressTimer = findViewById(R.id.progress_timer);
        btnStartPause = findViewById(R.id.btn_start_pause);

        String taskName = getIntent().getStringExtra("EXTRA_TASK_NAME");
        if (taskName != null) {
            textActiveTaskName.setText(taskName);
        }

        // Sync progress bar with total time
        progressTimer.setMax((int) START_TIME_IN_MILLIS);
        progressTimer.setProgress((int) timeLeftInMillis);
        updateCountDownText();

        btnStartPause.setOnClickListener(v -> {
            if (timerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
                progressTimer.setProgress((int) timeLeftInMillis);
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                btnStartPause.setText("Start");
                // Reset time for now (database logging will go here later)
                timeLeftInMillis = START_TIME_IN_MILLIS;
                progressTimer.setProgress((int) START_TIME_IN_MILLIS);
                updateCountDownText();
            }
        }.start();

        timerRunning = true;
        btnStartPause.setText("Pause");
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
        btnStartPause.setText("Start");
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        textCountdown.setText(timeLeftFormatted);
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, TimerService.class);
        serviceIntent.putExtra("EXTRA_TASK_NAME", textActiveTaskName.getText().toString());
        startService(serviceIntent);
    }

    private void stopForegroundService() {
        Intent serviceIntent = new Intent(this, TimerService.class);
        stopService(serviceIntent);
    }
}