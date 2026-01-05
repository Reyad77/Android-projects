package com.example.focusritualscheduler;

import android.app.DatePickerDialog;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class TasksListActivity extends AppCompatActivity {

    private EditText etTaskName, etPriority, etDueDate;
    private Button btnAddTask;
    private ListView lvTasks;

    public static ArrayList<String> taskList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static final String FILENAME = "tasks.txt";
    private static final String SEPARATOR = "\n---\n";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private Calendar selectedCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks_list);

        etTaskName = findViewById(R.id.et_task_name);
        etPriority = findViewById(R.id.et_priority);
        etDueDate = findViewById(R.id.et_due_day);
        btnAddTask = findViewById(R.id.btn_add_task);
        lvTasks = findViewById(R.id.lv_tasks);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, taskList);
        lvTasks.setAdapter(adapter);

        // Open calendar when tapping due date field
        etDueDate.setOnClickListener(v -> showDatePicker(etDueDate));

        btnAddTask.setOnClickListener(v -> addTask());

        lvTasks.setOnItemClickListener((parent, view, position, id) -> editTask(position));

        lvTasks.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure?")
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

        loadTasksFromFile();
        adapter.notifyDataSetChanged();
    }

    private void showDatePicker(EditText editText) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(year, month, dayOfMonth);
                    editText.setText(dateFormat.format(selectedCalendar.getTime()));
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); // No past dates
        datePickerDialog.show();
    }

    private void addTask() {
        String name = etTaskName.getText().toString().trim();
        String priorityStr = etPriority.getText().toString().trim();
        String dueDate = etDueDate.getText().toString().trim();

        if (name.isEmpty() || priorityStr.isEmpty() || dueDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int priority = Integer.parseInt(priorityStr);
        if (priority < 1 || priority > 5) {
            Toast.makeText(this, "Priority 1-5", Toast.LENGTH_SHORT).show();
            return;
        }

        String entry = name + "\nDue: " + dueDate + " | Priority: " + priority;

        taskList.add(entry);
        adapter.notifyDataSetChanged();
        clearInputs();
        saveTasksToFile();
        Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show();
    }

    private void editTask(int position) {
        String current = taskList.get(position);
        String[] lines = current.split("\n");
        String name = lines[0].trim();
        String[] details = lines[1].split(" \\| ");
        String dueDate = details[0].replace("Due: ", "").trim();
        String priorityStr = details[1].replace("Priority: ", "").trim();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_task, null);
        EditText editName = dialogView.findViewById(R.id.edit_task_name);
        EditText editDueDate = dialogView.findViewById(R.id.edit_due_day);
        EditText editPriority = dialogView.findViewById(R.id.edit_priority);

        editName.setText(name);
        editDueDate.setText(dueDate);
        editPriority.setText(priorityStr);

        // Make edit due date field open calendar
        editDueDate.setOnClickListener(v -> showDatePicker(editDueDate));

        new AlertDialog.Builder(this)
                .setTitle("Edit Task")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    String newDueDate = editDueDate.getText().toString().trim();
                    String newPriorityStr = editPriority.getText().toString().trim();

                    if (newName.isEmpty() || newDueDate.isEmpty() || newPriorityStr.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int newPriority = Integer.parseInt(newPriorityStr);
                    if (newPriority < 1 || newPriority > 5) {
                        Toast.makeText(this, "Priority 1-5", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newEntry = newName + "\nDue: " + newDueDate + " | Priority: " + newPriority;

                    taskList.set(position, newEntry);
                    adapter.notifyDataSetChanged();
                    saveTasksToFile();
                    Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearInputs() {
        etTaskName.setText("");
        etPriority.setText("");
        etDueDate.setText("");
    }

    private void saveTasksToFile() {
        try (FileOutputStream fos = openFileOutput(FILENAME, MODE_PRIVATE)) {
            for (String entry : taskList) {
                fos.write((entry + SEPARATOR).getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadTasksFromFile() {
        taskList.clear();
        try (FileInputStream fis = openFileInput(FILENAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
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
        } catch (Exception e) {
            // No file
        }
    }
}