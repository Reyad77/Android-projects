package com.example.focusritualscheduler;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class TimerActivity extends AppCompatActivity {

    private TextView tvTimer, tvRitualStep;
    private Button btnStartStop;
    private ProgressBar progressTimer;

    private CountDownTimer countDownTimer;
    private boolean isRunning = false;
    private long timeLeftInMillis = 25 * 60 * 1000; // 25 minutes default
    private int currentStepIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        tvTimer = findViewById(R.id.tv_timer);
        tvRitualStep = findViewById(R.id.tv_ritual_step);
        btnStartStop = findViewById(R.id.btn_start_stop);
        progressTimer = findViewById(R.id.progress_timer);

        updateTimerText();
        updateProgressBar();
        showNextRitualStep();

        btnStartStop.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        // Fade-in
        findViewById(R.id.content_layout).setAlpha(0f);
        findViewById(R.id.content_layout).animate().alpha(1f).setDuration(800).start();
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
                updateProgressBar();
            }

            @Override
            public void onFinish() {
                isRunning = false;
                timeLeftInMillis = 25 * 60 * 1000;
                updateTimerText();
                updateProgressBar();
                btnStartStop.setText("Start Focus Session");
                playCompletionSound();
                Toast.makeText(TimerActivity.this, "Focus session complete! Great job!", Toast.LENGTH_LONG).show();
                currentStepIndex = 0;
                showNextRitualStep();
            }
        }.start();

        isRunning = true;
        btnStartStop.setText("Pause");
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        btnStartStop.setText("Resume");
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvTimer.setText(timeFormatted);
    }

    private void updateProgressBar() {
        int progress = (int) ((25 * 60 * 1000 - timeLeftInMillis) * 100 / (25 * 60 * 1000));
        progressTimer.setProgress(progress);
    }

    private void showNextRitualStep() {
        if (RitualBuilderActivity.ritualSteps.isEmpty()) {
            tvRitualStep.setText("No ritual defined. Add steps in 'Build Focus Ritual'!");
            return;
        }

        if (currentStepIndex < RitualBuilderActivity.ritualSteps.size()) {
            tvRitualStep.setText(RitualBuilderActivity.ritualSteps.get(currentStepIndex));
            currentStepIndex++;
        } else {
            tvRitualStep.setText("Focus deeply now — you're in the zone!");
        }
    }

    private void playCompletionSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}