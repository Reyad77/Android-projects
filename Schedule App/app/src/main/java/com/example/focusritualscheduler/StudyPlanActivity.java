package com.example.focusritualscheduler;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.view.ViewGroup;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudyPlanActivity extends AppCompatActivity {

    private Button btnGeneratePlan;
    private Button btnDeleteCompleted;
    private ListView lvStudyPlan;
    private ArrayAdapter<String> planAdapter;
    private ArrayList<String> studyPlan = new ArrayList<>();
    private ArrayList<Boolean> checkedItems = new ArrayList<>();

    private static final String PLAN_FILENAME = "study_plan.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_plan);

        btnGeneratePlan = findViewById(R.id.btn_generate_plan);
        btnDeleteCompleted = findViewById(R.id.btn_delete_completed);
        lvStudyPlan = findViewById(R.id.lv_study_plan);

        // Custom adapter with checkboxes
        planAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, studyPlan) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
                view.setChecked(checkedItems.get(position));
                return view;
            }
        };
        lvStudyPlan.setAdapter(planAdapter);

        // Tap item to toggle checked state
        lvStudyPlan.setOnItemClickListener((parent, view, position, id) -> {
            boolean newState = !checkedItems.get(position);
            checkedItems.set(position, newState);
            planAdapter.notifyDataSetChanged();
            updateDeleteButton();
            saveStudyPlanToFile();
            Toast.makeText(this, newState ? "Marked as done ✓" : "Marked as undone", Toast.LENGTH_SHORT).show();
        });

        // Delete button click
        btnDeleteCompleted.setOnClickListener(v -> deleteCompletedTasks());

        btnGeneratePlan.setOnClickListener(v -> generateSmartStudyPlan());

        // Fade-in
        findViewById(R.id.content_layout).setAlpha(0f);
        findViewById(R.id.content_layout).animate().alpha(1f).setDuration(800).start();

        // Load saved data
        loadStudyPlanFromFile();
        planAdapter.notifyDataSetChanged();
        updateDeleteButton(); // Show/hide button on load
    }

    private void generateSmartStudyPlan() {
        studyPlan.clear();
        checkedItems.clear();

        // ... [same generation logic as before - parseClasses, parseTasks, assign slots] ...

        // (Keep your existing generateSmartStudyPlan body from previous version)

        planAdapter.notifyDataSetChanged();
        saveStudyPlanToFile();
        updateDeleteButton();
        Toast.makeText(this, "Study Plan Generated!", Toast.LENGTH_SHORT).show();
    }

    private void deleteCompletedTasks() {
        int removedCount = 0;
        for (int i = studyPlan.size() - 1; i >= 0; i--) {
            if (checkedItems.get(i)) {
                studyPlan.remove(i);
                checkedItems.remove(i);
                removedCount++;
            }
        }
        planAdapter.notifyDataSetChanged();
        saveStudyPlanToFile();
        updateDeleteButton();
        Toast.makeText(this, removedCount + " completed task(s) deleted", Toast.LENGTH_SHORT).show();
    }

    // Show/hide and update button text based on checked items
    private void updateDeleteButton() {
        long completedCount = checkedItems.stream().filter(Boolean::booleanValue).count();

        if (completedCount > 0) {
            btnDeleteCompleted.setText("Delete Completed (" + completedCount + ")");
            btnDeleteCompleted.setVisibility(View.VISIBLE);
        } else {
            btnDeleteCompleted.setVisibility(View.GONE);
        }
    }

    // Persistence methods (saveStudyPlanToFile, loadStudyPlanFromFile) remain the same
    private void saveStudyPlanToFile() {
        try {
            FileOutputStream fos = openFileOutput(PLAN_FILENAME, MODE_PRIVATE);
            for (int i = 0; i < studyPlan.size(); i++) {
                String line = studyPlan.get(i) + "|||CHECKED:" + checkedItems.get(i);
                fos.write((line + "\n").getBytes());
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStudyPlanFromFile() {
        studyPlan.clear();
        checkedItems.clear();
        try {
            FileInputStream fis = openFileInput(PLAN_FILENAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("|||CHECKED:")) {
                    String[] parts = line.split("\\|\\|\\|CHECKED:");
                    studyPlan.add(parts[0]);
                    checkedItems.add(Boolean.parseBoolean(parts[1]));
                } else {
                    studyPlan.add(line);
                    checkedItems.add(false);
                }
            }
            reader.close();
        } catch (Exception e) {
            // Normal on first launch
        }
    }

    // Keep all your parsing methods (parseClasses, findFreeSlots, parseTasks, etc.) exactly as before
    // ... (no changes needed here)

}