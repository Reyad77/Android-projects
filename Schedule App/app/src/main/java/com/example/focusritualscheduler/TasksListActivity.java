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
import java.util.ArrayList;

public class TasksListActivity extends AppCompatActivity {

    private EditText etTaskName, etEstimatedTime, etPriority;
    private Button btnAddTask;
    private ListView lvTasks;

    private ArrayList<String> taskList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks_list);

        etTaskName = findViewById(R.id.et_task_name);
        etEstimatedTime = findViewById(R.id.et_estimated_time);
        etPriority = findViewById(R.id.et_priority);
        btnAddTask = findViewById(R.id.btn_add_task);
        lvTasks = findViewById(R.id.lv_tasks);

        taskList = new ArrayList<>();
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
                        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        findViewById(R.id.content_layout).setAlpha(0f);
        findViewById(R.id.content_layout).animate().alpha(1f).setDuration(800).start();
    }

    private void addTask() {
        String name = etTaskName.getText().toString().trim();
        String timeStr = etEstimatedTime.getText().toString().trim();
        String priorityStr = etPriority.getText().toString().trim();

        if (name.isEmpty() || timeStr.isEmpty() || priorityStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int time = Integer.parseInt(timeStr);
        int priority = Integer.parseInt(priorityStr);

        if (priority < 1 || priority > 5) {
            Toast.makeText(this, "Priority must be 1-5", Toast.LENGTH_SHORT).show();
            return;
        }

        String entry = name + "\n⏱ " + time + " mins | Priority: " + priority;

        taskList.add(entry);
        adapter.notifyDataSetChanged();
        clearInputs();
        Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show();
    }

    private void editTask(int position) {
        String current = taskList.get(position);
        String[] lines = current.split("\n");
        String name = lines[0].trim();
        String[] details = lines[1].split(" \\| ");
        String timeStr = details[0].replace("⏱ ", "").replace(" mins", "").trim();
        String priorityStr = details[1].replace("Priority: ", "").trim();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_task, null);
        EditText editName = dialogView.findViewById(R.id.edit_task_name);
        EditText editTime = dialogView.findViewById(R.id.edit_estimated_time);
        EditText editPriority = dialogView.findViewById(R.id.edit_priority);

        editName.setText(name);
        editTime.setText(timeStr);
        editPriority.setText(priorityStr);

        new AlertDialog.Builder(this)
                .setTitle("Edit Task")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    String newTimeStr = editTime.getText().toString().trim();
                    String newPriorityStr = editPriority.getText().toString().trim();

                    if (newName.isEmpty() || newTimeStr.isEmpty() || newPriorityStr.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int newTime = Integer.parseInt(newTimeStr);
                    int newPriority = Integer.parseInt(newPriorityStr);

                    String newEntry = newName + "\n⏱ " + newTime + " mins | Priority: " + newPriority;

                    taskList.set(position, newEntry);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearInputs() {
        etTaskName.setText("");
        etEstimatedTime.setText("");
        etPriority.setText("");
    }
}