package com.example.pomodoro.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.example.pomodoro.MainActivity;
import com.example.pomodoro.R;
import com.example.pomodoro.data.TaskRepository;
import com.example.pomodoro.view.TimerActivity;
import com.example.pomodoro.model.Task;
import java.util.Locale;

public class TimerService extends Service {
    public static boolean isServiceRunning = false;

    public static final String ACTION_START = "START";
    public static final String ACTION_PAUSE = "PAUSE";
    public static final String ACTION_SKIP = "SKIP";
    public static final String ACTION_REQUEST_STATE = "REQUEST_STATE";
    public static final String ACTION_START_GRACE = "START_GRACE";
    public static final String ACTION_CANCEL_GRACE = "CANCEL_GRACE";
    public static final String ACTION_CLEAR_PENALTY = "CLEAR_PENALTY";
    public static final String ACTION_FORCE_STOP = "FORCE_STOP";
    public static final String UPDATE_BROADCAST = "com.example.pomodoro.TIMER_UPDATE";

    private static final String CHANNEL_ID = "TimerChannel";
    private static final String ALERT_CHANNEL_ID = "AlertChannel";

    private CountDownTimer timer;
    private TaskRepository repository;
    private NotificationManager notificationManager;

    private boolean isWorkSession = true;
    private boolean isRunning = false;
    private long timeLeftInMillis = 0;
    private long workTimeInMillis = 0;
    private long breakTimeInMillis = 0;

    private int taskId = -1;
    private String taskName = "";
    private int completedSessions = 0;
    private int estimatedSessions = 1;
    private boolean isStrictMode = false;
    private int storedWorkMins = 25;
    private int storedBreakMins = 5;
    private boolean isDevTask = false;

    private boolean penaltyApplied = false;
    private boolean wasPausedForCall = false;
    private Handler strictHandler = new Handler(Looper.getMainLooper());
    private Runnable strictRunnable = () -> {
        if (isWorkSession && isRunning) {
            if (timer != null) timer.cancel();
            isRunning = false;
            timeLeftInMillis = workTimeInMillis;
            penaltyApplied = true;
            updateForegroundNotification();
            sendHighPriorityAlert("Strict Mode Failed", "Focus lost! Timer reset.");
            broadcastUpdate(false, false);
        }
    };

    private BroadcastReceiver phoneStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE);
            if (android.telephony.TelephonyManager.EXTRA_STATE_RINGING.equals(state) ||
                    android.telephony.TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {

                strictHandler.removeCallbacks(strictRunnable);

                if (isRunning && isWorkSession) {
                    wasPausedForCall = true;
                    pauseTimer();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        repository = new TaskRepository(getApplication());
        notificationManager = getSystemService(NotificationManager.class);
        createNotificationChannels();

        ContextCompat.registerReceiver(
                this,
                phoneStateReceiver,
                new IntentFilter("android.intent.action.PHONE_STATE"),
                ContextCompat.RECEIVER_EXPORTED
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {

            Task task = (Task) intent.getSerializableExtra("EXTRA_TASK");
            if (task != null) {
                boolean isTaskChanging = (taskId != -1 && taskId != task.getId());

                taskId = task.getId();
                taskName = task.getName();
                estimatedSessions = task.getEstimatedPomodoros();
                storedWorkMins = task.getWorkDuration();
                storedBreakMins = task.getBreakDuration();
                isDevTask = task.isDevTask();

                completedSessions = intent.getIntExtra("EXTRA_COMPLETED", completedSessions);
                isStrictMode = intent.getBooleanExtra("EXTRA_STRICT_MODE", isStrictMode);

                long newWorkTime = isDevTask ? storedWorkMins * 1000L : storedWorkMins * 60000L;
                long newBreakTime = isDevTask ? storedBreakMins * 1000L : storedBreakMins * 60000L;

                if (isTaskChanging || timeLeftInMillis == 0 || workTimeInMillis != newWorkTime || breakTimeInMillis != newBreakTime) {
                    if (timer != null) timer.cancel();
                    isRunning = false;
                    isWorkSession = true;
                    timeLeftInMillis = newWorkTime;
                }

                workTimeInMillis = newWorkTime;
                breakTimeInMillis = newBreakTime;
            }

            switch (intent.getAction()) {
                case ACTION_START:
                    wasPausedForCall = false;
                    penaltyApplied = false;
                    if (!isRunning) {
                        saveActiveTaskState();
                        startForegroundNotification();
                        startTimer();
                    }
                    break;
                case ACTION_PAUSE:
                    pauseTimer();
                    break;
                case ACTION_SKIP:
                    if (timer != null) timer.cancel();
                    isRunning = false;

                    if (isWorkSession) {
                        isWorkSession = false;
                        timeLeftInMillis = breakTimeInMillis;
                        saveActiveTaskState();
                        startForegroundNotification();
                        startTimer();
                    } else {
                        isWorkSession = true;
                        timeLeftInMillis = workTimeInMillis;
                        broadcastUpdate(false, true);
                        stopForeground(true);
                    }
                    break;
                case ACTION_REQUEST_STATE:
                    broadcastUpdate(false, false);
                    break;
                case ACTION_START_GRACE:
                    if (isStrictMode && isWorkSession && isRunning) {
                        strictHandler.postDelayed(strictRunnable, 5000);
                    }
                    break;
                case ACTION_CANCEL_GRACE:
                    strictHandler.removeCallbacks(strictRunnable);
                    break;
                case ACTION_CLEAR_PENALTY:
                    penaltyApplied = false;
                    break;
                case ACTION_FORCE_STOP:
                    if (timer != null) timer.cancel();
                    isRunning = false;
                    timeLeftInMillis = 0;
                    workTimeInMillis = 0;
                    breakTimeInMillis = 0;
                    taskId = -1;
                    clearActiveTaskState();
                    stopForeground(true);
                    stopSelf();
                    break;
            }
        }
        return START_STICKY;
    }

    private void saveActiveTaskState() {
        SharedPreferences prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putInt("activeTaskId", taskId)
                .putString("activeTaskName", taskName)
                .putInt("activeTaskCompleted", completedSessions)
                .putInt("activeTaskEstimated", estimatedSessions)
                .putBoolean("activeTaskStrict", isStrictMode)
                .putInt("activeTaskWorkDur", storedWorkMins)
                .putInt("activeTaskBreakDur", storedBreakMins)
                .putBoolean("activeTaskIsDev", isDevTask)
                .apply();
    }

    private void clearActiveTaskState() {
        SharedPreferences prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE);
        prefs.edit().putInt("activeTaskId", -1).apply();
    }

    private void startTimer() {
        isRunning = true;
        timer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateForegroundNotification();
                broadcastUpdate(false, false);
            }

            @Override
            public void onFinish() {
                handleSessionEnd();
            }
        }.start();
        broadcastUpdate(false, false);
    }

    private void pauseTimer() {
        if (timer != null) timer.cancel();
        isRunning = false;
        updateForegroundNotification();
        broadcastUpdate(false, false);
    }

    private void handleSessionEnd() {
        isRunning = false;

        if (isWorkSession) {
            completedSessions++;
            saveActiveTaskState();
            if (taskId != -1) repository.incrementPomodoroCount(taskId);

            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
            repository.logSession(today, taskName, storedWorkMins);

            if (completedSessions >= estimatedSessions) {
                clearActiveTaskState();
                sendHighPriorityAlert("Task Complete!", "You have finished all Pomodoros for this task.");
                broadcastUpdate(true, false);
                stopForeground(true);
                stopSelf();
                return;
            }

            sendHighPriorityAlert("Work Complete!", "Time for your break.");
            isWorkSession = false;
            timeLeftInMillis = breakTimeInMillis;
            startTimer();
        } else {
            sendHighPriorityAlert("Break Over!", "Ready to focus again?");
            isWorkSession = true;
            timeLeftInMillis = workTimeInMillis;
            broadcastUpdate(false, true);
            stopForeground(true);
        }
    }

    private void broadcastUpdate(boolean taskFinished, boolean breakJustFinished) {
        Intent intent = new Intent(UPDATE_BROADCAST);
        intent.setPackage(getPackageName());
        intent.putExtra("TIME_LEFT", timeLeftInMillis);
        intent.putExtra("MAX_TIME", isWorkSession ? workTimeInMillis : breakTimeInMillis);
        intent.putExtra("IS_WORK", isWorkSession);
        intent.putExtra("IS_RUNNING", isRunning);
        intent.putExtra("COMPLETED_COUNT", completedSessions);
        intent.putExtra("TASK_FINISHED", taskFinished);
        intent.putExtra("BREAK_JUST_FINISHED", breakJustFinished);
        intent.putExtra("STRICT_PENALTY_APPLIED", penaltyApplied);
        intent.putExtra("PAUSED_FOR_CALL", wasPausedForCall);
        sendBroadcast(intent);
    }

    private void startForegroundNotification() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }
    }

    private void updateForegroundNotification() {
        notificationManager.notify(1, buildNotification());
    }

    private Notification buildNotification() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        String state = isWorkSession ? "Working on: " : "Break from: ";

        Intent clickIntent = new Intent(this, MainActivity.class);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(state + taskName)
                .setContentText("Time left: " + timeStr)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .build();
    }

    private void sendHighPriorityAlert(String title, String message) {
        Intent clickIntent = new Intent(this, MainActivity.class);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder alert = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL);
        notificationManager.notify(2, alert.build());
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel bgChannel = new NotificationChannel(
                    CHANNEL_ID, "Timer Service", NotificationManager.IMPORTANCE_LOW);

            NotificationChannel alertChannel = new NotificationChannel(
                    ALERT_CHANNEL_ID, "Session Alerts", NotificationManager.IMPORTANCE_HIGH);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(bgChannel);
                notificationManager.createNotificationChannel(alertChannel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        if (timer != null) timer.cancel();
        unregisterReceiver(phoneStateReceiver);
        super.onDestroy();
    }
}