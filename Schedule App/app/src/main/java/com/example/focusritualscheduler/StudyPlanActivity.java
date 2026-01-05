package com.example.focusritualscheduler;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
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

    private ArrayAdapter<String> planAdapter;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    private static final String PLAN_FILE = "study_plan.txt";
    private static final String SEPARATOR = "\n---\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_plan);

        btnGeneratePlan = findViewById(R.id.btn_generate_plan);
        btnDeleteCompleted = findViewById(R.id.btn_delete_completed);
        lvStudyPlan = findViewById(R.id.lv_study_plan);

        // Use built-in layout that works perfectly with Material themes
        planAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, studyPlan);
        lvStudyPlan.setAdapter(planAdapter);
        lvStudyPlan.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        lvStudyPlan.setOnItemClickListener((parent, view, position, id) -> {
            if (position < checkedItems.size()) {
                checkedItems.set(position, !checkedItems.get(position));
                updateDeleteButton();
                savePlanToFile();
            }
        });

        btnGeneratePlan.setOnClickListener(v -> generatePlan());
        btnDeleteCompleted.setOnClickListener(v -> deleteCompleted());

        loadPlanFromFile();
        updateDeleteButton();

        // Fade-in animation
        View content = findViewById(R.id.content_layout);
        if (content != null) {
            content.setAlpha(0f);
            content.animate().alpha(1f).setDuration(800).start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePlanToFile();
    }

    private void generatePlan() {
        ArrayList<String> tasks = new ArrayList<>();
        if (TasksListActivity.taskList != null) {
            tasks.addAll(TasksListActivity.taskList);
        }

        if (tasks.isEmpty()) {
            Toast.makeText(this, "Add some tasks in 'Weekly Tasks' first!", Toast.LENGTH_LONG).show();
            return;
        }

        studyPlan.clear();
        checkedItems.clear();

        TreeMap<Date, String> freeSlots = getFreeSlots();
        ArrayList<Task> taskObjects = parseTasks(tasks);

        Collections.sort(taskObjects, (a, b) -> {
            int dateCompare = a.dueDate.compareTo(b.dueDate);
            if (dateCompare != 0) return dateCompare;
            return Integer.compare(b.priority, a.priority);
        });

        for (Task task : taskObjects) {
            boolean assigned = false;
            for (Date slotStart : new ArrayList<>(freeSlots.keySet())) {
                if (!slotStart.after(task.dueDate)) {
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(slotStart);
                    endCal.add(Calendar.HOUR_OF_DAY, 1);

                    String dayLine = dayFormat.format(slotStart);
                    String timeRange = timeFormat.format(slotStart) + " - " + timeFormat.format(endCal.getTime());

                    String planEntry = dayLine + "\n" + timeRange + "\n" + task.name +
                            "\nPriority: " + task.priority + " | Due: " + dateFormat.format(task.dueDate);

                    studyPlan.add(planEntry);
                    checkedItems.add(false);
                    freeSlots.remove(slotStart);
                    assigned = true;
                    break;
                }
            }
            if (!assigned) {
                studyPlan.add("⚠️ No free slot found:\n" + task.name +
                        "\nPriority: " + task.priority + " | Due: " + dateFormat.format(task.dueDate));
                checkedItems.add(false);
            }
        }

        planAdapter.notifyDataSetChanged();
        updateDeleteButton();
        savePlanToFile();
        Toast.makeText(this, "Study plan generated!", Toast.LENGTH_SHORT).show();
    }

    private void deleteCompleted() {
        for (int i = checkedItems.size() - 1; i >= 0; i--) {
            if (checkedItems.get(i)) {
                studyPlan.remove(i);
                checkedItems.remove(i);
            }
        }
        planAdapter.notifyDataSetChanged();
        updateDeleteButton();
        savePlanToFile();
        Toast.makeText(this, "Completed tasks deleted!", Toast.LENGTH_SHORT).show();
    }

    private void updateDeleteButton() {
        int count = 0;
        for (boolean checked : checkedItems) {
            if (checked) count++;
        }
        if (count > 0) {
            btnDeleteCompleted.setText("Delete Completed (" + count + ")");
            btnDeleteCompleted.setVisibility(View.VISIBLE);
        } else {
            btnDeleteCompleted.setVisibility(View.GONE);
        }
    }

    private TreeMap<Date, String> getFreeSlots() {
        TreeMap<Date, String> slots = new TreeMap<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 18);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 21; i++) {
            slots.put((Date) cal.getTime().clone(), "Evening Study");
            cal.add(Calendar.HOUR_OF_DAY, 1);
        }
        return slots;
    }

    private ArrayList<Task> parseTasks(ArrayList<String> tasks) {
        ArrayList<Task> taskObjects = new ArrayList<>();
        for (String entry : tasks) {
            try {
                String[] lines = entry.split("\n");
                if (lines.length < 2) continue;
                String name = lines[0].trim();
                String[] details = lines[1].split(" \\| ");
                if (details.length < 2) continue;
                String dueStr = details[0].replace("Due: ", "").trim();
                int priority = Integer.parseInt(details[1].replace("Priority: ", "").trim());
                Date dueDate = dateFormat.parse(dueStr);
                taskObjects.add(new Task(name, priority, dueDate));
            } catch (Exception ignored) {}
        }
        return taskObjects;
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
                        String text = isDone ? content.substring(5) : content.substring(5);
                        studyPlan.add(text);
                        checkedItems.add(isDone);
                        sb = new StringBuilder();
                    }
                } else {
                    sb.append(line).append("\n");
                }
            }
            if (sb.length() > 0) {
                String content = sb.toString().trim();
                boolean isDone = content.startsWith("DONE:");
                String text = isDone ? content.substring(5) : content.substring(5);
                studyPlan.add(text);
                checkedItems.add(isDone);
            }
        } catch (Exception e) {
            // No saved plan yet
        }
        planAdapter.notifyDataSetChanged();
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