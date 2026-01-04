package com.example.focusritualscheduler;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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

        planAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, studyPlan) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckedTextView view = (CheckedTextView) super.getView(position, convertView, parent);
                view.setChecked(checkedItems.get(position));
                return view;
            }
        };
        lvStudyPlan.setAdapter(planAdapter);

        lvStudyPlan.setOnItemClickListener((parent, view, position, id) -> {
            boolean newState = !checkedItems.get(position);
            checkedItems.set(position, newState);
            planAdapter.notifyDataSetChanged();
            updateDeleteButton();
            saveStudyPlanToFile();
            Toast.makeText(this, newState ? "Great job! Completed ✓" : "Task reopened", Toast.LENGTH_SHORT).show();
        });

        btnGeneratePlan.setOnClickListener(v -> generateSmartStudyPlan());

        btnDeleteCompleted.setOnClickListener(v -> deleteCompletedTasks());

        // Fade-in
        findViewById(R.id.content_layout).setAlpha(0f);
        findViewById(R.id.content_layout).animate().alpha(1f).setDuration(800).start();

        loadStudyPlanFromFile();
        planAdapter.notifyDataSetChanged();
        updateDeleteButton();
    }

    private void generateSmartStudyPlan() {
        studyPlan.clear();
        checkedItems.clear();

        if (ScheduleEditorActivity.classList.isEmpty() && TasksListActivity.taskList.isEmpty()) {
            studyPlan.add("No classes or tasks added yet.\nAdd them first!");
            checkedItems.add(false);
        } else {
            Map<String, List<int[]>> occupiedSlots = parseClasses();
            List<Task> tasks = parseTasks();

            if (tasks.isEmpty()) {
                studyPlan.add("No weekly tasks added.\nGo to Weekly Tasks and add some!");
                checkedItems.add(false);
            } else {
                tasks.sort((a, b) -> Integer.compare(b.priority, a.priority));

                Map<String, List<int[]>> freeSlots = findFreeSlots(occupiedSlots);

                for (Task task : tasks) {
                    boolean assigned = false;
                    for (String day : getDayOrder()) {
                        if (getDayOfWeekFromName(day) > getDayOfWeekFromName(task.dueDay)) continue;

                        List<int[]> slots = freeSlots.getOrDefault(day, new ArrayList<>());
                        for (int i = 0; i < slots.size(); i++) {
                            int[] slot = slots.get(i);
                            int duration = slot[1] - slot[0];
                            if (duration >= task.timeNeeded) {
                                String startTime = minutesToTime(slot[0]);
                                String endTime = minutesToTime(slot[0] + task.timeNeeded);
                                studyPlan.add(day + " " + startTime + " - " + endTime + "\nStudy: " + task.name + " (due " + task.dueDay + ")");
                                checkedItems.add(false);
                                slots.set(i, new int[]{slot[0] + task.timeNeeded, slot[1]});
                                assigned = true;
                                break;
                            }
                        }
                        if (assigned) break;
                    }
                    if (!assigned) {
                        studyPlan.add("⚠ No free slot before due date:\n" + task.name + " (" + task.timeNeeded + " mins, due " + task.dueDay + ")");
                        checkedItems.add(false);
                    }
                }
            }
        }

        if (studyPlan.isEmpty()) {
            studyPlan.add("All tasks successfully assigned! Great planning 🎉");
            checkedItems.add(false);
        }

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

    private void updateDeleteButton() {
        long completedCount = checkedItems.stream().filter(b -> b).count();

        if (completedCount > 0) {
            btnDeleteCompleted.setText("Delete Completed (" + completedCount + ")");
            btnDeleteCompleted.setVisibility(View.VISIBLE);
        } else {
            btnDeleteCompleted.setVisibility(View.GONE);
        }
    }

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

    // ---------- PARSING METHODS (NOW INCLUDED) ----------

    private Map<String, List<int[]>> parseClasses() {
        Map<String, List<int[]>> occupied = new HashMap<>();
        for (String entry : ScheduleEditorActivity.classList) {
            try {
                String[] lines = entry.split("\n");
                if (lines.length < 2) continue;
                String[] firstLine = lines[0].split(" \\| ");
                if (firstLine.length < 2) continue;
                String day = firstLine[0].trim();
                String timeLine = lines[1].trim();
                String[] times = timeLine.split(" - ");
                if (times.length < 2) continue;
                int startMin = timeToMinutes(times[0].trim());
                int endMin = timeToMinutes(times[1].trim());
                occupied.computeIfAbsent(day, k -> new ArrayList<>()).add(new int[]{startMin, endMin});
            } catch (Exception ignored) {}
        }
        for (List<int[]> slots : occupied.values()) {
            slots.sort((a, b) -> Integer.compare(a[0], b[0]));
        }
        return occupied;
    }

    private Map<String, List<int[]>> findFreeSlots(Map<String, List<int[]>> occupied) {
        Map<String, List<int[]>> free = new HashMap<>();
        int dayStart = 480;  // 08:00
        int dayEnd = 1320;   // 22:00

        for (String day : getDayOrder()) {
            List<int[]> classes = occupied.getOrDefault(day, new ArrayList<>());
            List<int[]> slots = new ArrayList<>();
            int lastEnd = dayStart;

            for (int[] cls : classes) {
                if (cls[0] - lastEnd >= 60) {
                    slots.add(new int[]{lastEnd, cls[0]});
                }
                lastEnd = Math.max(lastEnd, cls[1]);
            }
            if (dayEnd - lastEnd >= 60) {
                slots.add(new int[]{lastEnd, dayEnd});
            }
            if (!slots.isEmpty()) {
                free.put(day, slots);
            }
        }
        return free;
    }

    private List<Task> parseTasks() {
        List<Task> tasks = new ArrayList<>();
        for (String entry : TasksListActivity.taskList) {
            try {
                String[] lines = entry.split("\n");
                if (lines.length < 2) continue;
                String name = lines[0].trim();
                String[] details = lines[1].split(" \\| ");
                if (details.length < 3) continue;
                String dueDay = details[0].replace("Due: ", "").trim();
                int time = Integer.parseInt(details[1].replace("⏱ ", "").replace(" mins", "").trim());
                int priority = Integer.parseInt(details[2].replace("Priority: ", "").trim());
                tasks.add(new Task(name, time, priority, dueDay));
            } catch (Exception ignored) {}
        }
        return tasks;
    }

    private List<String> getDayOrder() {
        return List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    }

    private int timeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private String minutesToTime(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }

    private int getDayOfWeekFromName(String dayName) {
        if (dayName == null || dayName.trim().isEmpty()) return -1;
        String normalized = dayName.trim().toLowerCase(Locale.getDefault());

        if (normalized.equals("monday")) return 1;
        if (normalized.equals("tuesday")) return 2;
        if (normalized.equals("wednesday")) return 3;
        if (normalized.equals("thursday")) return 4;
        if (normalized.equals("friday")) return 5;
        if (normalized.equals("saturday")) return 6;
        if (normalized.equals("sunday")) return 0;

        return -1;
    }

    private static class Task {
        String name;
        int timeNeeded;
        int priority;
        String dueDay;

        Task(String name, int timeNeeded, int priority, String dueDay) {
            this.name = name;
            this.timeNeeded = timeNeeded;
            this.priority = priority;
            this.dueDay = dueDay;
        }
    }
}