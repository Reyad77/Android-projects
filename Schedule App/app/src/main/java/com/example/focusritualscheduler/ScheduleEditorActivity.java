package com.example.focusritualscheduler;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class ScheduleEditorActivity extends AppCompatActivity {

    private EditText etDay, etStartTime, etEndTime, etSubject, etLocation;
    private Button btnAddClass;
    private ListView lvClasses;

    private ArrayList<String> classList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_editor);

        // Find views
        etDay = findViewById(R.id.et_day);
        etStartTime = findViewById(R.id.et_start_time);
        etEndTime = findViewById(R.id.et_end_time);
        etSubject = findViewById(R.id.et_subject);
        etLocation = findViewById(R.id.et_location);
        btnAddClass = findViewById(R.id.btn_add_class);
        lvClasses = findViewById(R.id.lv_classes);

        // Setup list
        classList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, classList);
        lvClasses.setAdapter(adapter);

        // Add button click
        btnAddClass.setOnClickListener(v -> addClass());

        // Fade-in animation (same as main)
        findViewById(R.id.content_layout).setAlpha(0f);
        findViewById(R.id.content_layout).animate().alpha(1f).setDuration(800).start();
    }

    private void addClass() {
        String day = etDay.getText().toString().trim();
        String start = etStartTime.getText().toString().trim();
        String end = etEndTime.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (day.isEmpty() || start.isEmpty() || end.isEmpty() || subject.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String entry = day + " | " + subject + "\n" + start + " - " + end +
                (location.isEmpty() ? "" : "\nRoom: " + location);

        classList.add(entry);
        adapter.notifyDataSetChanged();

        // Clear inputs
        etDay.setText("");
        etStartTime.setText("");
        etEndTime.setText("");
        etSubject.setText("");
        etLocation.setText("");

        Toast.makeText(this, "Class added!", Toast.LENGTH_SHORT).show();
    }
}