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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

public class StudyPlanActivity extends AppCompatActivity {

    private Button btnGeneratePlan, btnDeleteCompleted;
    private ListView lvStudyPlan;
    private ArrayList<String> studyPlan = new ArrayList<>();
    private ArrayList<Boolean> checkedItems = new ArrayList<>();

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat fullFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());

    private static final String PLAN_FILE = "study_plan.txt";
    private static final String SEPARATOR = "\n---\n";  // Fixed: Added missing constant

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_plan);

        btnGeneratePlan = findViewById(R.id.btn_generate_plan);
        btnDeleteCompleted = findViewById(R.id.btn_delete_completed);
        lvStudyPlan = findViewById(R.id.lv_study_plan);

        lvStudyPlan.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        lvStudyPlan.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, studyPlan) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
                view.setChecked(checkedItems.get(position));
                return view;
            }
        });

        lvStudyPlan.setOnItemClickListener((parent, view, position, id) -> {
            boolean newState = !checkedItems.get(position);
            checkedItems.set(position, newState);
            ((ArrayAdapter<?>) lvStudyPlan.getAdapter()).notifyDataSetChanged();
            updateDeleteButton();
            savePlanToFile();
            Toast.makeText(this, newState ? "Marked as completed ✓" : "Reopened task", Toast.LENGTH_SHORT).show();
        });

        btnGeneratePlan.setOnClickListener(v -> generateSmartStudyPlan());

        btnDeleteCompleted.setOnClickListener(v -> deleteCompletedTasks());

        // Load saved plan and checked states
        loadPlanFromFile();
        updateDeleteButton();

        // Fade-in animation
        findViewById(R.id.content_layout).setAlpha(0f);
        findViewById(R.id.content_layout).animate().alpha(1f).setDuration(800).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePlanToFile();  // Save whenever leaving the screen
    }

    private void generateSmartStudyPlan() {
        studyPlan.clear();
        checkedItems.clear();

        if (TasksListActivity.taskList.isEmpty()) {
            Toast.makeText(this, "Add some tasks first!", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayList<Task> tasks = parseTasks();
        if (tasks.isEmpty()) {
            Toast.makeText(this, "No valid tasks with due dates found", Toast.LENGTH_LONG).show();
            return;
        }

        // Sort by priority (high first), then by due date
        Collections.sort(tasks, (a, b) -> {
            if (a.priority != b.priority) return Integer.compare(b.priority, a.priority);
            return a.dueDate.compareTo(b.dueDate);
        });

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 18); // Start suggesting from 6 PM
        cal.set(Calendar.MINUTE, 0);

        for (Task task : tasks) {
            Calendar taskCal = Calendar.getInstance();
            taskCal.setTime(task.dueDate);

            // Suggest study 1-3 days before due date
            taskCal.add(Calendar.DAY_OF_YEAR, -1 - (5 - task.priority)); // Higher priority = earlier suggestion

            if (taskCal.before(Calendar.getInstance())) {
                taskCal = Calendar.getInstance(); // If overdue, suggest today
            }

            taskCal.set(Calendar.HOUR_OF_DAY, 18 + (studyPlan.size() % 3)); // 6-8 PM slots
            taskCal.set(Calendar.MINUTE, 0);

            String planEntry = fullFormat.format(taskCal.getTime()) + " • 1 hour\n" +
                    task.name + "\nPriority: " + task.priority + " | Due: " + dateFormat.format(task.dueDate);

            studyPlan.add(planEntry);
            checkedItems.add(false);
        }

        ((ArrayAdapter<?>) lvStudyPlan.getAdapter()).notifyDataSetChanged();
        updateDeleteButton();
        savePlanToFile();

        Toast.makeText(this, "Smart study plan generated!", Toast.LENGTH_SHORT).show();
    }

    private void deleteCompletedTasks() {
        int deletedCount = 0;
        for (int i = checkedItems.size() - 1; i >= 0; i--) {
            if (checkedItems.get(i)) {
                studyPlan.remove(i);
                checkedItems.remove(i);
                deletedCount++;
            }
        }
        ((ArrayAdapter<?>) lvStudyPlan.getAdapter()).notifyDataSetChanged();
        updateDeleteButton();
        savePlanToFile();
        Toast.makeText(this, deletedCount + " completed task(s) deleted", Toast.LENGTH_SHORT).show();
    }

    private void updateDeleteButton() {
        int completedCount = 0;
        for (Boolean checked : checkedItems) {
            if (checked) completedCount++;
        }
        btnDeleteCompleted.setText("Delete Completed (" + completedCount + ")");
        btnDeleteCompleted.setVisibility(completedCount > 0 ? View.VISIBLE : View.GONE);
    }

    private ArrayList<Task> parseTasks() {
        ArrayList<Task> tasks = new ArrayList<>();
        for (String entry : TasksListActivity.taskList) {
            try {
                String[] lines = entry.split("\n");
                if (lines.length < 2) continue;

                String name = lines[0].trim();
                String[] details = lines[1].split(" \\| ");
                if (details.length < 2) continue;

                String dueStr = details[0].replace("Due: ", "").trim();
                int priority = Integer.parseInt(details[1].replace("Priority: ", "").trim());

                Date dueDate = dateFormat.parse(dueStr);
                if (dueDate != null) {
                    tasks.add(new Task(name, priority, dueDate));
                }
            } catch (Exception ignored) {}
        }
        return tasks;
    }

    private void savePlanToFile() {
        try (FileOutputStream fos = openFileOutput(PLAN_FILE, MODE_PRIVATE)) {
            for (int i = 0; i < studyPlan.size(); i++) {
                String prefix = checkedItems.get(i) ? "DONE:" : "TODO:";
                fos.write((prefix + studyPlan.get(i) + SEPARATOR).getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPlanFromFile() {
        studyPlan.clear();
        checkedItems.clear();
        try (FileInputStream fis = openFileInput(PLAN_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    if (sb.length() > 0) {
                        String content = sb.toString().trim();
                        boolean isDone = content.startsWith("DONE:");
                        if (isDone) content = content.substring(5);
                        else if (content.startsWith("TODO:")) content = content.substring(5);

                        studyPlan.add(content);
                        checkedItems.add(isDone);
                        sb = new StringBuilder();
                    }
                } else {
                    sb.append(line).append("\n");
                }
            }
            // Last item
            if (sb.length() > 0) {
                String content = sb.toString().trim();
                boolean isDone = content.startsWith("DONE:");
                if (isDone) content = content.substring(5);
                else if (content.startsWith("TODO:")) content = content.substring(5);

                studyPlan.add(content);
                checkedItems.add(isDone);
            }
        } catch (Exception e) {
            // No saved plan yet
        }
        ((ArrayAdapter<?>) lvStudyPlan.getAdapter()).notifyDataSetChanged();
    }

    private static class Task {
        String name;
        int priority;
        Date dueDate;

        Task(String name, int priority, Date dueDate) {
            this.name = name;
            this.priority = priority;
            this.dueDate = dueDate;
        }
    }
}