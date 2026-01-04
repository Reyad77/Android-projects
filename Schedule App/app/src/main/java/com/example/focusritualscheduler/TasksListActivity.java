package com.example.focusritualscheduler;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class TasksListActivity extends AppCompatActivity {

    private EditText etTaskName, etEstimatedTime, etPriority, etDueDay;
    private Button btnAddTask;
    private ListView lvTasks;

    public static ArrayList<String> taskList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static final String FILENAME = "tasks.txt";
    private static final String SEPARATOR = "\n---\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks_list);

        etTaskName = findViewById(R.id.et_task_name);
        etEstimatedTime = findViewById(R.id.et_estimated_time);
        etPriority = findViewById(R.id.et_priority);
        etDueDay = findViewById(R.id.et_due_day); // New field
        btnAddTask = findViewById(R.id.btn_add_task);
        lvTasks = findViewById(R.id.lv_tasks);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, taskList);
        lvTasks.setAdapter(adapter);

        btnAddTask.setOnClickListener(v -> addTask());

        // Normal click → Edit
        lvTasks.setOnItemClickListener((parent, view, position, id) -> editTask(position));

        // Long click → Delete
        lvTasks.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        taskList.remove(position);
                        adapter.notifyDataSetChanged();
                        saveTasksToFile();
                        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        findViewById(R.id.content_layout).setAlpha(0f);
        findViewById(R.id.content_layout).animate().alpha(1f).setDuration(800).start();

        // Load saved tasks
        loadTasksFromFile();
        adapter.notifyDataSetChanged();
    }

    private void addTask() {
        String name = etTaskName.getText().toString().trim();
        String timeStr = etEstimatedTime.getText().toString().trim();
        String priorityStr = etPriority.getText().toString().trim();
        String dueDay = etDueDay.getText().toString().trim();

        if (name.isEmpty() || timeStr.isEmpty() || priorityStr.isEmpty() || dueDay.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int time = Integer.parseInt(timeStr);
        int priority = Integer.parseInt(priorityStr);

        if (priority < 1 || priority > 5) {
            Toast.makeText(this, "Priority must be 1-5", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate due day
        if (getDayOfWeekFromName(dueDay) == -1) {
            Toast.makeText(this, "Invalid due day: '" + dueDay + "'. Use Monday to Sunday.", Toast.LENGTH_LONG).show();
            return;
        }

        String entry = name + "\nDue: " + dueDay + " | ⏱ " + time + " mins | Priority: " + priority;

        taskList.add(entry);
        adapter.notifyDataSetChanged();
        clearInputs();
        Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show();

        saveTasksToFile();
    }

    private void editTask(int position) {
        String current = taskList.get(position);
        String[] lines = current.split("\n");
        String name = lines[0].trim();
        String[] details = lines[1].split(" \\| ");
        String dueDay = details[0].replace("Due: ", "").trim();
        String timeStr = details[1].replace("⏱ ", "").replace(" mins", "").trim();
        String priorityStr = details[2].replace("Priority: ", "").trim();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_task, null);
        EditText editName = dialogView.findViewById(R.id.edit_task_name);
        EditText editTime = dialogView.findViewById(R.id.edit_estimated_time);
        EditText editPriority = dialogView.findViewById(R.id.edit_priority);
        EditText editDueDay = dialogView.findViewById(R.id.edit_due_day); // New field

        editName.setText(name);
        editTime.setText(timeStr);
        editPriority.setText(priorityStr);
        editDueDay.setText(dueDay);

        new AlertDialog.Builder(this)
                .setTitle("Edit Task")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    String newTimeStr = editTime.getText().toString().trim();
                    String newPriorityStr = editPriority.getText().toString().trim();
                    String newDueDay = editDueDay.getText().toString().trim();

                    if (newName.isEmpty() || newTimeStr.isEmpty() || newPriorityStr.isEmpty() || newDueDay.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int newTime = Integer.parseInt(newTimeStr);
                    int newPriority = Integer.parseInt(newPriorityStr);

                    if (getDayOfWeekFromName(newDueDay) == -1) {
                        Toast.makeText(this, "Invalid due day: '" + newDueDay + "'. Use Monday to Sunday.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String newEntry = newName + "\nDue: " + newDueDay + " | ⏱ " + newTime + " mins | Priority: " + newPriority;

                    taskList.set(position, newEntry);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();

                    saveTasksToFile();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearInputs() {
        etTaskName.setText("");
        etEstimatedTime.setText("");
        etPriority.setText("");
        etDueDay.setText("");
    }

    private void saveTasksToFile() {
        try {
            FileOutputStream fos = openFileOutput(FILENAME, MODE_PRIVATE);
            for (String entry : taskList) {
                fos.write((entry + SEPARATOR).getBytes());
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTasksFromFile() {
        taskList.clear();
        try {
            FileInputStream fis = openFileInput(FILENAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    if (sb.length() > 0) {
                        taskList.add(sb.toString().trim());
                        sb = new StringBuilder();
                    }
                } else {
                    sb.append(line).append("\n");
                }
            }
            if (sb.length() > 0) {
                taskList.add(sb.toString().trim());
            }
            reader.close();
        } catch (Exception e) {
            // No file yet
        }
    }

    // Helper method for day validation
    private int getDayOfWeekFromName(String dayName) {
        if (dayName == null || dayName.trim().isEmpty()) return -1;

        String normalized = dayName.trim().toLowerCase(Locale.getDefault());

        if (normalized.equals("monday")) return Calendar.MONDAY;
        if (normalized.equals("tuesday")) return Calendar.TUESDAY;
        if (normalized.equals("wednesday")) return Calendar.WEDNESDAY;
        if (normalized.equals("thursday")) return Calendar.THURSDAY;
        if (normalized.equals("friday")) return Calendar.FRIDAY;
        if (normalized.equals("saturday")) return Calendar.SATURDAY;
        if (normalized.equals("sunday")) return Calendar.SUNDAY;

        return -1;
    }
}