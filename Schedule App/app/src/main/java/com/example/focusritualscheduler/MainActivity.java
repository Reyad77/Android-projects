package com.example.focusritualscheduler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.content_layout).setAlpha(0f);
        findViewById(R.id.content_layout).animate().alpha(1f).setDuration(1000).start();
        Button btnEditSchedule = findViewById(R.id.btn_edit_schedule);
        Button btnWeeklyTasks = findViewById(R.id.btn_weekly_tasks);
        Button btnGeneratePlan = findViewById(R.id.btn_generate_plan);
        Button btnBuildRitual = findViewById(R.id.btn_build_ritual);
        Button btnStartSession = findViewById(R.id.btn_start_session);

        btnEditSchedule.setOnClickListener(v -> startActivity(new Intent(this, ScheduleEditorActivity.class)));
        btnWeeklyTasks.setOnClickListener(v -> startActivity(new Intent(this, TasksListActivity.class)));
        btnGeneratePlan.setOnClickListener(v -> startActivity(new Intent(this, StudyPlanActivity.class)));
        btnBuildRitual.setOnClickListener(v -> startActivity(new Intent(this, RitualBuilderActivity.class)));
        btnStartSession.setOnClickListener(v -> startActivity(new Intent(this, TimerActivity.class)));
    }
}