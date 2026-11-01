package com.example.pomodoro.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.pomodoro.R;
import com.example.pomodoro.model.Quote;
import com.example.pomodoro.network.QuoteApi;
import com.example.pomodoro.network.RetrofitClient;
import com.example.pomodoro.service.TimerService;
import com.example.pomodoro.model.Task;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimerActivity extends AppCompatActivity {

    private TextView textCountdown, textActiveTaskName, textQuote, textSessionCount;
    private ProgressBar progressTimer;
    private Button btnStartPause, btnSkip;

    private Task currentTask;
    private int currentCompletedSessions;
    private boolean isStrictMode;
    private boolean currentlyOnBreak = false;
    private boolean isTimerRunning = false;
    private long maxTimerValue = 1500000;

    private boolean penaltyDialogShowing = false;
    private boolean callDialogShowing = false;
    private boolean breakDialogShowing = false;

    private Handler quoteHandler = new Handler(Looper.getMainLooper());
    private int currentQuoteIndex = 0;
    private List<String> quoteList = new ArrayList<>();

    private Runnable quoteRunnable = new Runnable() {
        @Override
        public void run() {
            if (quoteList.size() > 1) {
                textQuote.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                    currentQuoteIndex = (currentQuoteIndex + 1) % quoteList.size();
                    textQuote.setText(quoteList.get(currentQuoteIndex));
                    textQuote.animate().alpha(1f).setDuration(500);
                });
            }
            quoteHandler.postDelayed(this, 45000);
        }
    };

    private BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long timeLeft = intent.getLongExtra("TIME_LEFT", 0);
            maxTimerValue = intent.getLongExtra("MAX_TIME", 1500000);
            boolean isWork = intent.getBooleanExtra("IS_WORK", true);
            isTimerRunning = intent.getBooleanExtra("IS_RUNNING", false);

            currentCompletedSessions = intent.getIntExtra("COMPLETED_COUNT", currentCompletedSessions);

            boolean hasPenalty = intent.getBooleanExtra("STRICT_PENALTY_APPLIED", false);
            boolean pausedForCall = intent.getBooleanExtra("PAUSED_FOR_CALL", false);

            if (intent.getBooleanExtra("TASK_FINISHED", false)) {
                updateUI(0, true, false, currentCompletedSessions);
                showCongratsDialog();
                return;
            }
            if (intent.getBooleanExtra("BREAK_JUST_FINISHED", false) && !breakDialogShowing) {
                breakDialogShowing = true;
                showReadyToFocusDialog();
            }

            if (hasPenalty && !penaltyDialogShowing) {
                penaltyDialogShowing = true;
                sendCommandToService(TimerService.ACTION_CLEAR_PENALTY);
                new androidx.appcompat.app.AlertDialog.Builder(TimerActivity.this)
                        .setTitle("Strict Mode Failed")
                        .setMessage("You lost focus for too long! Your timer has been paused and reset.")
                        .setCancelable(false)
                        .setPositiveButton("I Understand", (d, w) -> penaltyDialogShowing = false)
                        .show();
            }

            if (pausedForCall && !isTimerRunning && !callDialogShowing && !currentlyOnBreak) {
                callDialogShowing = true;
                new androidx.appcompat.app.AlertDialog.Builder(TimerActivity.this)
                        .setTitle("Phone Call Ended")
                        .setMessage("Your timer was paused for a phone call. Ready to resume?")
                        .setCancelable(false)
                        .setPositiveButton("Resume Focus", (d, w) -> {
                            callDialogShowing = false;
                            sendCommandToService(TimerService.ACTION_START);
                        })
                        .show();
            }

            updateUI(timeLeft, isWork, isTimerRunning, currentCompletedSessions);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        currentTask = (Task) getIntent().getSerializableExtra("EXTRA_TASK");
        if (currentTask != null) {
            currentCompletedSessions = currentTask.getCompletedPomodoros();
        }
        isStrictMode = getIntent().getBooleanExtra("EXTRA_STRICT_MODE", false);

        SharedPreferences quotePrefs = getSharedPreferences("quote_cache", MODE_PRIVATE);
        Set<String> savedQuotes = quotePrefs.getStringSet("saved_quotes", new HashSet<>());
        quoteList = new ArrayList<>(savedQuotes);

        textActiveTaskName = findViewById(R.id.text_active_task_name);
        textCountdown = findViewById(R.id.text_countdown);
        textSessionCount = findViewById(R.id.text_session_count);
        textQuote = findViewById(R.id.text_quote);
        progressTimer = findViewById(R.id.progress_timer);
        btnStartPause = findViewById(R.id.btn_start_pause);
        btnSkip = findViewById(R.id.btn_skip);

        if (currentTask != null) {
            textSessionCount.setText("Session: " + currentCompletedSessions + " / " + currentTask.getEstimatedPomodoros());
            textActiveTaskName.setText(currentTask.getName());

            long initialTime = currentTask.getWorkDuration() * (currentTask.isDevTask() ? 1000L : 60000L);
            progressTimer.setMax((int) initialTime);
            progressTimer.setProgress((int) initialTime);
            int minutes = (int) (initialTime / 1000) / 60;
            int seconds = (int) (initialTime / 1000) % 60;
            textCountdown.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds));
        }

        btnStartPause.setOnClickListener(v -> {
            if (btnStartPause.getText().toString().equals("Pause")) {
                sendCommandToService(TimerService.ACTION_PAUSE);
            } else {
                sendCommandToService(TimerService.ACTION_START);
            }
        });

        btnSkip.setOnClickListener(v -> sendCommandToService(TimerService.ACTION_SKIP));
    }

    @Override
    protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(
                this,
                timerReceiver,
                new IntentFilter(TimerService.UPDATE_BROADCAST),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        sendCommandToService(TimerService.ACTION_REQUEST_STATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sendCommandToService(TimerService.ACTION_START_GRACE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendCommandToService(TimerService.ACTION_CANCEL_GRACE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(timerReceiver);
        stopQuoteCycling();
    }

    private void updateUI(long timeLeft, boolean isWork, boolean isRunning, int completed) {
        if (currentTask != null) {
            textSessionCount.setText("Session: " + completed + " / " + currentTask.getEstimatedPomodoros());
        }

        progressTimer.setMax((int) maxTimerValue);
        progressTimer.setProgress((int) timeLeft);

        int minutes = (int) (timeLeft / 1000) / 60;
        int seconds = (int) (timeLeft / 1000) % 60;
        textCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        if (!isWork && !currentlyOnBreak) {
            currentlyOnBreak = true;
            textActiveTaskName.setText("Break Time!");
            if (!quoteList.isEmpty()) {
                textQuote.setText(quoteList.get(0));
            } else {
                textQuote.setText("");
            }
            fetchAPIQuote();
            startQuoteCycling();
        } else if (isWork && currentlyOnBreak) {
            currentlyOnBreak = false;
            if (currentTask != null) textActiveTaskName.setText(currentTask.getName());
            stopQuoteCycling();
        }

        if (isRunning) {
            if (isStrictMode || !isWork) {
                btnStartPause.setVisibility(View.GONE);
            } else {
                btnStartPause.setVisibility(View.VISIBLE);
                btnStartPause.setText("Pause");
            }
        } else {
            btnStartPause.setVisibility(View.VISIBLE);
            btnStartPause.setText("Start");
        }
    }

    private void startQuoteCycling() {
        quoteHandler.removeCallbacks(quoteRunnable);
        quoteHandler.postDelayed(quoteRunnable, 45000);
    }

    private void stopQuoteCycling() {
        quoteHandler.removeCallbacks(quoteRunnable);
        textQuote.animate().cancel();
        textQuote.setAlpha(1f);
        textQuote.setText("");
    }

    private void fetchAPIQuote() {
        QuoteApi quoteApi = RetrofitClient.getClient().create(QuoteApi.class);
        quoteApi.getRandomQuote().enqueue(new Callback<List<Quote>>() {
            @Override
            public void onResponse(Call<List<Quote>> call, Response<List<Quote>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Quote quote = response.body().get(0);
                    String newQuote = "\"" + quote.getText() + "\"\n- " + quote.getAuthor();

                    if (!quoteList.contains(newQuote)) {
                        quoteList.add(newQuote);
                        if (quoteList.size() > 5) quoteList.remove(0);

                        SharedPreferences prefs = getSharedPreferences("quote_cache", MODE_PRIVATE);
                        prefs.edit().putStringSet("saved_quotes", new HashSet<>(quoteList)).apply();
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Quote>> call, Throwable t) {}
        });
    }

    private void showReadyToFocusDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Break is over")
                .setMessage("Are you ready to start your next focus session?")
                .setCancelable(false)
                .setPositiveButton("Let's Go", (dialog, which) -> {
                    breakDialogShowing = false;
                    sendCommandToService(TimerService.ACTION_START);
                })
                .setNegativeButton("Wait", (dialog, which) -> breakDialogShowing = false)
                .show();
    }

    private void showCongratsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Task Complete!")
                .setMessage("You have completed all planned Pomodoro sessions for this task.")
                .setCancelable(false)
                .setPositiveButton("Awesome", (dialog, which) -> finish())
                .show();
    }

    private void sendCommandToService(String action) {
        Intent intent = new Intent(this, TimerService.class);
        intent.setAction(action);

        if (currentTask != null) {
            currentTask.setCompletedPomodoros(currentCompletedSessions);
            intent.putExtra("EXTRA_TASK", currentTask);
            intent.putExtra("EXTRA_TASK_ID", currentTask.getId());
            intent.putExtra("EXTRA_IS_DEV_TASK", currentTask.isDevTask());
        }
        intent.putExtra("EXTRA_COMPLETED", currentCompletedSessions);
        intent.putExtra("EXTRA_STRICT_MODE", isStrictMode);

        startService(intent);
    }
}