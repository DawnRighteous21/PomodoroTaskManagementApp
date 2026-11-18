package com.example.pomodoro.view;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.widget.ImageButton;
import android.widget.Toast;
import java.util.HashMap;
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
import android.animation.ObjectAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
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

    private com.airbnb.lottie.LottieAnimationView lottieAnimation;
    private TextView textCountdown, textActiveTaskName, textQuote, textSessionCount, textStrictIndicator;
    private ProgressBar progressTimer;
    private ObjectAnimator progressAnimator;
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
    private ImageButton btnFavoriteQuote;
    private DatabaseReference mDatabase;
    private Quote currentDisplayedQuote = null;
    private boolean isCurrentQuoteSaved = false;

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

        Toolbar toolbar = findViewById(R.id.toolbar_timer);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        currentTask = (Task) getIntent().getSerializableExtra("EXTRA_TASK");
        if (currentTask != null) {
            currentCompletedSessions = currentTask.getCompletedPomodoros();
        }
        isStrictMode = getIntent().getBooleanExtra("EXTRA_STRICT_MODE", false);

        SharedPreferences quotePrefs = getSharedPreferences("quote_cache", MODE_PRIVATE);
        Set<String> savedQuotes = quotePrefs.getStringSet("saved_quotes", new HashSet<>());
        quoteList = new ArrayList<>(savedQuotes);

        lottieAnimation = findViewById(R.id.lottie_animation);
        textStrictIndicator = findViewById(R.id.text_strict_indicator);
        textActiveTaskName = findViewById(R.id.text_active_task_name);
        textCountdown = findViewById(R.id.text_countdown);
        textSessionCount = findViewById(R.id.text_session_count);
        textQuote = findViewById(R.id.text_quote);
        progressTimer = findViewById(R.id.progress_timer);
        btnStartPause = findViewById(R.id.btn_start_pause);
        btnSkip = findViewById(R.id.btn_skip);
        btnFavoriteQuote = findViewById(R.id.btn_favorite_quote);

        SharedPreferences userPrefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE);
        String userId = userPrefs.getString("user_device_id", null);
        if (userId == null) {
            userId = java.util.UUID.randomUUID().toString();
            userPrefs.edit().putString("user_device_id", userId).apply();
        }

        String dbUrl = "https://pomodoro-3cf47-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("favorite_quotes").child(userId);
        btnFavoriteQuote.setOnClickListener(v -> saveQuoteToFirebase());
        mDatabase.keepSynced(true);
        textStrictIndicator.setVisibility(isStrictMode ? View.VISIBLE : View.GONE);

        if (currentTask != null) {
            textSessionCount.setText("Session: " + currentCompletedSessions + " / " + currentTask.getEstimatedPomodoros());
            textActiveTaskName.setText(currentTask.getName());

            long initialTime = currentTask.getWorkDuration() * (currentTask.isDevTask() ? 1000L : 60000L);
            progressTimer.setMax((int) initialTime);
            progressTimer.setProgress((int) initialTime);
            long timeInSeconds = (long) Math.ceil(initialTime / 1000.0);
            int minutes = (int) (timeInSeconds / 60);
            int seconds = (int) (timeInSeconds % 60);
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
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
    }

    private void updateUI(long timeLeft, boolean isWork, boolean isRunning, int completed) {
        if (currentTask != null) {
            textSessionCount.setText("Session: " + completed + " / " + currentTask.getEstimatedPomodoros());
        }

        progressTimer.setMax((int) maxTimerValue);

        if (isRunning) {
            if (progressAnimator != null && progressAnimator.isRunning()) {
                progressAnimator.cancel();
            }
            long targetProgress = Math.max(0, timeLeft - 1000);
            if (Math.abs(progressTimer.getProgress() - timeLeft) > 2000) {
                progressTimer.setProgress((int) timeLeft);
            }
            progressAnimator = ObjectAnimator.ofInt(progressTimer, "progress", progressTimer.getProgress(), (int) targetProgress);
            progressAnimator.setDuration(1000);
            progressAnimator.setInterpolator(new LinearInterpolator());
            progressAnimator.start();
        } else {
            if (progressAnimator != null) {
                progressAnimator.cancel();
            }
            progressTimer.setProgress((int) timeLeft);
        }

        long timeInSeconds = (long) Math.ceil(timeLeft / 1000.0);
        int minutes = (int) (timeInSeconds / 60);
        int seconds = (int) (timeInSeconds % 60);
        textCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        if (!isWork) {
            if (!currentlyOnBreak) {
                currentlyOnBreak = true;
                if (currentTask != null) textActiveTaskName.setText("Break Time!");

                if (lottieAnimation != null) lottieAnimation.setVisibility(View.VISIBLE);

                if (textQuote != null) textQuote.setText("Đang tải trích dẫn...");
                if (btnFavoriteQuote != null) btnFavoriteQuote.setVisibility(View.GONE);

                startQuoteCycling();
                fetchAPIQuote();
            }
        } else {
            if (currentlyOnBreak) {
                currentlyOnBreak = false;
                if (lottieAnimation != null) lottieAnimation.setVisibility(View.GONE);
                if (currentTask != null) textActiveTaskName.setText(currentTask.getName());

                stopQuoteCycling();
                if (btnFavoriteQuote != null) btnFavoriteQuote.setVisibility(View.GONE);
            }
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
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        currentDisplayedQuote = response.body().get(0);

                        if (currentDisplayedQuote.getText() == null || currentDisplayedQuote.getText().contains("Too many requests")) {
                            handleQuoteError("API đang bận. Hãy nghỉ ngơi thật tốt nhé!");
                            return;
                        }

                        String newQuote = "\"" + currentDisplayedQuote.getText() + "\"\n- " + currentDisplayedQuote.getAuthor();

                        if (textQuote != null) {
                            textQuote.setText(newQuote);
                        }

                        if (btnFavoriteQuote != null && currentlyOnBreak) {
                            btnFavoriteQuote.setVisibility(View.VISIBLE);
                            btnFavoriteQuote.setImageResource(android.R.drawable.star_off);
                        }
                        isCurrentQuoteSaved = false;

                        if (!quoteList.contains(newQuote)) {
                            quoteList.add(newQuote);
                            if (quoteList.size() > 5) quoteList.remove(0);

                            SharedPreferences prefs = getSharedPreferences("quote_cache", MODE_PRIVATE);
                            prefs.edit().putStringSet("saved_quotes", new HashSet<>(quoteList)).apply();
                        }
                    } else {
                        handleQuoteError("Không thể tải trích dẫn lúc này.\nHãy nhắm mắt thư giãn nhé!");
                    }
                });
            }

            @Override
            public void onFailure(Call<List<Quote>> call, Throwable t) {
                runOnUiThread(() -> {
                    handleQuoteError("Mất kết nối mạng.\nHãy uống một ngụm nước nhé!");
                    android.util.Log.e("API_ERROR", "Lỗi tải quote", t);
                });
            }
        });
    }

    private void handleQuoteError(String fallbackMessage) {
        if (textQuote != null) {
            textQuote.setText(fallbackMessage);
        }
        if (btnFavoriteQuote != null) {
            btnFavoriteQuote.setVisibility(View.GONE);
        }
        currentDisplayedQuote = null;
    }

    private void saveQuoteToFirebase() {
        if (currentDisplayedQuote == null) {
            Toast.makeText(this, "Lỗi: Dữ liệu Quote đang trống (null)!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCurrentQuoteSaved) {
            Toast.makeText(this, "Bạn đã lưu câu này rồi!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Đang gửi lên Firebase...", Toast.LENGTH_SHORT).show();

        String pushId = mDatabase.push().getKey();
        if (pushId != null) {
            HashMap<String, String> quoteData = new HashMap<>();
            quoteData.put("text", currentDisplayedQuote.getText());
            quoteData.put("author", currentDisplayedQuote.getAuthor());

            mDatabase.child(pushId).setValue(quoteData, (error, ref) -> {
                if (error != null) {
                    Toast.makeText(TimerActivity.this, "Lỗi Firebase: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    android.util.Log.e("FIREBASE_TEST", "Lỗi: " + error.getMessage());
                } else {
                    btnFavoriteQuote.setImageResource(android.R.drawable.star_on);
                    isCurrentQuoteSaved = true;
                    Toast.makeText(TimerActivity.this, "Đã lưu vào mây thành công!", Toast.LENGTH_SHORT).show();
                    android.util.Log.d("FIREBASE_TEST", "Lưu thành công ID: " + pushId);
                }
            });
        }
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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}