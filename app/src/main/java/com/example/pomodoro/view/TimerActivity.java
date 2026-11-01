package com.example.pomodoro.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.pomodoro.R;
import com.example.pomodoro.model.Quote;
import com.example.pomodoro.network.QuoteApi;
import com.example.pomodoro.network.RetrofitClient;
import com.example.pomodoro.service.TimerService;
import com.example.pomodoro.viewmodel.TaskViewModel;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimerActivity extends AppCompatActivity {
    private static final long WORK_TIME_IN_MILLIS = 1500000;
    private static final long BREAK_TIME_IN_MILLIS = 300000;

    private TextView textCountdown;
    private TextView textActiveTaskName;
    private TextView textQuote;
    private ProgressBar progressTimer;
    private Button btnStartPause;
    private Button btnSkip;

    private CountDownTimer countDownTimer;
    private boolean timerRunning;
    private boolean isWorkSession = true;
    private long timeLeftInMillis = WORK_TIME_IN_MILLIS;
    private String currentTaskName;
    private TaskViewModel taskViewModel;
    private int currentTaskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        currentTaskId = getIntent().getIntExtra("EXTRA_TASK_ID", -1);

        textActiveTaskName = findViewById(R.id.text_active_task_name);
        textCountdown = findViewById(R.id.text_countdown);
        textQuote = findViewById(R.id.text_quote);
        progressTimer = findViewById(R.id.progress_timer);
        btnStartPause = findViewById(R.id.btn_start_pause);
        btnSkip = findViewById(R.id.btn_skip);

        currentTaskName = getIntent().getStringExtra("EXTRA_TASK_NAME");
        if (currentTaskName != null) {
            textActiveTaskName.setText(currentTaskName);
        }

        progressTimer.setMax((int) WORK_TIME_IN_MILLIS);
        progressTimer.setProgress((int) timeLeftInMillis);
        updateCountDownText();

        btnStartPause.setOnClickListener(v -> {
            if (timerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnSkip.setOnClickListener(v -> {
            if(countDownTimer != null) countDownTimer.cancel();
            triggerSessionEnd();
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
                triggerSessionEnd();
            }
        }.start();

        timerRunning = true;
        btnStartPause.setText("Pause");
        startForegroundService();
    }

    private void triggerSessionEnd() {
        timerRunning = false;
        btnStartPause.setText("Start");
        stopForegroundService();

        if (isWorkSession) {
            if (currentTaskId != -1) {
                taskViewModel.incrementPomodoroCount(currentTaskId);
            }
            isWorkSession = false;
            timeLeftInMillis = BREAK_TIME_IN_MILLIS;
            progressTimer.setMax((int) BREAK_TIME_IN_MILLIS);
            textActiveTaskName.setText("Break Time!");
            fetchQuote();
        } else {
            isWorkSession = true;
            timeLeftInMillis = WORK_TIME_IN_MILLIS;
            progressTimer.setMax((int) WORK_TIME_IN_MILLIS);
            textActiveTaskName.setText(currentTaskName != null ? currentTaskName : "Work Time");
            textQuote.setText("");
        }

        progressTimer.setProgress((int) timeLeftInMillis);
        updateCountDownText();
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
        btnStartPause.setText("Start");
        stopForegroundService();
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        textCountdown.setText(timeLeftFormatted);
    }

    private void fetchQuote() {
        textQuote.setText("Fetching quote...");
        QuoteApi quoteApi = RetrofitClient.getClient().create(QuoteApi.class);
        Call<List<Quote>> call = quoteApi.getRandomQuote();

        call.enqueue(new Callback<List<Quote>>() {
            @Override
            public void onResponse(Call<List<Quote>> call, Response<List<Quote>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Quote quote = response.body().get(0);
                    String quoteText = "\"" + quote.getText() + "\"\n- " + quote.getAuthor();
                    textQuote.setText(quoteText);
                } else {
                    textQuote.setText("Take a deep breath and relax.");
                }
            }

            @Override
            public void onFailure(Call<List<Quote>> call, Throwable t) {
                Log.e("TimerActivity", "API Fetch Failed", t);
                textQuote.setText("Enjoy your break!");
            }
        });
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
