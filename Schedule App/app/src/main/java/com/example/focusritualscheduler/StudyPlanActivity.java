package com.example.focusritualscheduler;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyPlanActivity extends AppCompatActivity {

    private Button btnGeneratePlan;
    private ListView lvStudyPlan;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> planList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_plan); // New layout we'll create

        btnGeneratePlan = findViewById(R.id.btn_generate_plan);
        lvStudyPlan = findViewById(R.id.lv_study_plan);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, planList);
        lvStudyPlan.setAdapter(adapter);

        btnGeneratePlan.setOnClickListener(v -> generateStudyPlan());

        // Fade-in
        findViewById(R.id.content_layout).setAlpha(0f);
        findViewById(R.id.content_layout).animate().alpha(1f).setDuration(800).start();
    }

    private void generateStudyPlan() {
        planList.clear();

        // Step 1: Parse classes and find free slots per day
        Map<String, List<int[]>> classTimes = parseClasses(); // day -> list of [startMin, endMin]
        Map<String, List<int[]>> freeSlots = findFreeSlots(classTimes); // day -> list of [startMin, endMin] gaps >60min

        // Step 2: Parse and sort tasks by priority (descending)
        List<Task> tasks = parseTasks();
        Collections.sort(tasks, (a, b) -> b.priority - a.priority); // Highest priority first

        // Step 3: Assign tasks to free slots (simple: fit into largest slots first)
        for (Task task : tasks) {
            boolean assigned = false;
            for (String day : new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"}) {
                List<int[]> slots = freeSlots.getOrDefault(day, new ArrayList<>());
                for (int i = 0; i < slots.size(); i++) {
                    int[] slot = slots.get(i);
                    int slotDuration = slot[1] - slot[0];
                    if (slotDuration >= task.timeNeeded) {
                        // Assign
                        String startTime = minutesToTime(slot[0]);
                        String endTime = minutesToTime(slot[0] + task.timeNeeded);
                        String entry = day + " " + startTime + " - " + endTime + ": " + task.name;
                        planList.add(entry);

                        // Update slot (shorten it)
                        slots.set(i, new int[]{slot[0] + task.timeNeeded, slot[1]});
                        assigned = true;
                        break;
                    }
                }
                if (assigned) break;
            }
            if (!assigned) {
                planList.add("Unassigned: " + task.name + " (No free slot found)");
            }
        }

        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Study Plan Generated!", Toast.LENGTH_SHORT).show();
    }

    // Helper: Parse classList into day -> list of [startMin, endMin]
    private Map<String, List<int[]>> parseClasses() {
        Map<String, List<int[]>> classTimes = new HashMap<>();
        for (String entry : ScheduleEditorActivity.classList) {
            String[] lines = entry.split("\n");
            String[] firstLine = lines[0].split(" \\| ");
            String day = firstLine[0].trim();
            String[] times = lines[1].split(" - ");
            int startMin = timeToMinutes(times[0].trim());
            int endMin = timeToMinutes(times[1].trim());

            classTimes.computeIfAbsent(day, k -> new ArrayList<>()).add(new int[]{startMin, endMin});
        }
        // Sort classes per day by start time
        for (List<int[]> times : classTimes.values()) {
            times.sort(Comparator.comparingInt(a -> a[0]));
        }
        return classTimes;
    }

    // Helper: Find free slots >60min (assume day starts 8AM=480, ends 10PM=1320)
    private Map<String, List<int[]>> findFreeSlots(Map<String, List<int[]>> classTimes) {
        Map<String, List<int[]>> freeSlots = new HashMap<>();
        int dayStart = 480; // 8:00 AM in minutes
        int dayEnd = 1320; // 10:00 PM

        for (String day : new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"}) {
            List<int[]> classes = classTimes.getOrDefault(day, new ArrayList<>());
            List<int[]> slots = new ArrayList<>();
            int lastEnd = dayStart;

            for (int[] cls : classes) {
                if (cls[0] - lastEnd >= 60) {
                    slots.add(new int[]{lastEnd, cls[0]});
                }
                lastEnd = cls[1];
            }
            if (dayEnd - lastEnd >= 60) {
                slots.add(new int[]{lastEnd, dayEnd});
            }
            freeSlots.put(day, slots);
        }
        return freeSlots;
    }

    // Helper: Parse taskList into Task objects
    private List<Task> parseTasks() {
        List<Task> tasks = new ArrayList<>();
        for (String entry : TasksListActivity.taskList) {
            String[] lines = entry.split("\n");
            String name = lines[0].trim();
            String[] details = lines[1].split(" \\| ");
            int time = Integer.parseInt(details[0].replace("⏱ ", "").replace(" mins", "").trim());
            int priority = Integer.parseInt(details[1].replace("Priority: ", "").trim());
            tasks.add(new Task(name, time, priority));
        }
        return tasks;
    }

    // Helper: Convert "HH:MM" to minutes since midnight
    private int timeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    // Helper: Convert minutes to "HH:MM"
    private String minutesToTime(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    // Inner class for tasks
    private static class Task {
        String name;
        int timeNeeded;
        int priority;

        Task(String name, int timeNeeded, int priority) {
            this.name = name;
            this.timeNeeded = timeNeeded;
            this.priority = priority;
        }
    }
}