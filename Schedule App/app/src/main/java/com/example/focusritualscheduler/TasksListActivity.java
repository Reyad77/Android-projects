package com.example.focusritualscheduler;

import android.os.Bundle;
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

        // Long click → Show confirmation dialog
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

        // Fade-in animation
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

        etTaskName.setText("");
        etEstimatedTime.setText("");
        etPriority.setText("");

        Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show();
    }
}