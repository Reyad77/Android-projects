package com.example.focusritualscheduler;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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

        etDay = findViewById(R.id.et_day);
        etStartTime = findViewById(R.id.et_start_time);
        etEndTime = findViewById(R.id.et_end_time);
        etSubject = findViewById(R.id.et_subject);
        etLocation = findViewById(R.id.et_location);
        btnAddClass = findViewById(R.id.btn_add_class);
        lvClasses = findViewById(R.id.lv_classes);

        classList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, classList);
        lvClasses.setAdapter(adapter);

        btnAddClass.setOnClickListener(v -> addClass());

        // Normal click → Edit
        lvClasses.setOnItemClickListener((parent, view, position, id) -> editClass(position));

        // Long click → Delete with confirmation
        lvClasses.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Class")
                    .setMessage("Are you sure you want to delete this class?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        classList.remove(position);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Class deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        // Fade-in
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
        clearInputs();
        Toast.makeText(this, "Class added!", Toast.LENGTH_SHORT).show();
    }

    private void editClass(int position) {
        String currentEntry = classList.get(position);

        // Parse current values (simple string split)
        String[] lines = currentEntry.split("\n");
        String[] firstLine = lines[0].split(" \\| ");
        String day = firstLine[0].trim();
        String subject = firstLine[1].trim();
        String time = lines[1].trim(); // "09:00 - 10:30"
        String[] times = time.split(" - ");
        String start = times[0].trim();
        String end = times[1].trim();
        String location = lines.length > 2 ? lines[2].replace("Room: ", "").trim() : "";

        // Inflate custom edit dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_class, null);
        EditText editDay = dialogView.findViewById(R.id.edit_day);
        EditText editStart = dialogView.findViewById(R.id.edit_start_time);
        EditText editEnd = dialogView.findViewById(R.id.edit_end_time);
        EditText editSubject = dialogView.findViewById(R.id.edit_subject);
        EditText editLocation = dialogView.findViewById(R.id.edit_location);

        editDay.setText(day);
        editStart.setText(start);
        editEnd.setText(end);
        editSubject.setText(subject);
        editLocation.setText(location);

        new AlertDialog.Builder(this)
                .setTitle("Edit Class")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newDay = editDay.getText().toString().trim();
                    String newStart = editStart.getText().toString().trim();
                    String newEnd = editEnd.getText().toString().trim();
                    String newSubject = editSubject.getText().toString().trim();
                    String newLocation = editLocation.getText().toString().trim();

                    if (newDay.isEmpty() || newStart.isEmpty() || newEnd.isEmpty() || newSubject.isEmpty()) {
                        Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newEntry = newDay + " | " + newSubject + "\n" + newStart + " - " + newEnd +
                            (newLocation.isEmpty() ? "" : "\nRoom: " + newLocation);

                    classList.set(position, newEntry);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Class updated!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearInputs() {
        etDay.setText("");
        etStartTime.setText("");
        etEndTime.setText("");
        etSubject.setText("");
        etLocation.setText("");
    }
}