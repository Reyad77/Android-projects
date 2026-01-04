package com.example.focusritualscheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ScheduleEditorActivity extends AppCompatActivity {

    private EditText etDay, etStartTime, etEndTime, etSubject, etLocation;
    private Button btnAddClass;
    private ListView lvClasses;

    public static ArrayList<String> classList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private AlarmManager alarmManager;

    private static final String FILENAME = "classes.txt";
    private static final String SEPARATOR = "\n---\n";

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

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, classList);
        lvClasses.setAdapter(adapter);

        btnAddClass.setOnClickListener(v -> addClass());

        lvClasses.setOnItemClickListener((parent, view, position, id) -> editClass(position));

        lvClasses.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Class")
                    .setMessage("Are you sure you want to delete this class?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        classList.remove(position);
                        adapter.notifyDataSetChanged();
                        saveClassesToFile();
                        cancelAllAlarms();
                        scheduleAllReminders();
                        Toast.makeText(this, "Class deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        // Fade-in animation
        findViewById(R.id.content_layout).setAlpha(0f);
        findViewById(R.id.content_layout).animate().alpha(1f).setDuration(800).start();

        // Load saved classes from file
        loadClassesFromFile();
        adapter.notifyDataSetChanged();

        // Schedule reminders
        scheduleAllReminders();
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

        if (getDayOfWeekFromName(day) == -1) {
            Toast.makeText(this, "Invalid day: '" + day + "'. Use Monday to Sunday.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!start.matches("\\d{2}:\\d{2}") || !end.matches("\\d{2}:\\d{2}")) {
            Toast.makeText(this, "Time format must be HH:MM (e.g., 09:00)", Toast.LENGTH_SHORT).show();
            return;
        }

        String entry = day + " | " + subject + "\n" + start + " - " + end +
                (location.isEmpty() ? "" : "\nRoom: " + location);

        classList.add(entry);
        adapter.notifyDataSetChanged();
        clearInputs();
        Toast.makeText(this, "Class added!", Toast.LENGTH_SHORT).show();

        saveClassesToFile();
        cancelAllAlarms();
        scheduleAllReminders();
    }

    private void editClass(int position) {
        String currentEntry = classList.get(position);

        String[] lines = currentEntry.split("\n");
        String[] firstLine = lines[0].split(" \\| ");
        String day = firstLine[0].trim();
        String subject = firstLine[1].trim();
        String time = lines[1].trim();
        String[] times = time.split(" - ");
        String start = times[0].trim();
        String end = times[1].trim();
        String location = lines.length > 2 ? lines[2].replace("Room: ", "").trim() : "";

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

                    if (getDayOfWeekFromName(newDay) == -1) {
                        Toast.makeText(this, "Invalid day: '" + newDay + "'. Use Monday to Sunday.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!newStart.matches("\\d{2}:\\d{2}") || !newEnd.matches("\\d{2}:\\d{2}")) {
                        Toast.makeText(this, "Time format must be HH:MM", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newEntry = newDay + " | " + newSubject + "\n" + newStart + " - " + newEnd +
                            (newLocation.isEmpty() ? "" : "\nRoom: " + newLocation);

                    classList.set(position, newEntry);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Class updated!", Toast.LENGTH_SHORT).show();

                    saveClassesToFile();
                    cancelAllAlarms();
                    scheduleAllReminders();
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

    // Save classes to internal file
    private void saveClassesToFile() {
        try {
            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            for (String entry : classList) {
                fos.write((entry + SEPARATOR).getBytes());
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load classes from internal file
    private void loadClassesFromFile() {
        classList.clear();
        try {
            FileInputStream fis = openFileInput(FILENAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    if (sb.length() > 0) {
                        classList.add(sb.toString().trim());
                        sb = new StringBuilder();
                    }
                } else {
                    sb.append(line).append("\n");
                }
            }
            if (sb.length() > 0) {
                classList.add(sb.toString().trim());
            }
            reader.close();
        } catch (Exception e) {
            // File doesn't exist yet — normal on first launch
        }
    }

    private void cancelAllAlarms() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        for (int code = 1000; code < 1010; code++) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, code, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
            }
        }
    }

    private void scheduleAllReminders() {
        int requestCode = 1000;

        for (String entry : new ArrayList<>(classList)) {
            try {
                String[] lines = entry.split("\n");
                if (lines.length < 2) continue;

                String[] firstLineParts = lines[0].split(" \\| ");
                if (firstLineParts.length < 2) continue;

                String dayName = firstLineParts[0].trim();
                String subject = firstLineParts[1].trim();

                String[] timeParts = lines[1].split(" - ");
                if (timeParts.length < 2) continue;

                String startTime = timeParts[0].trim();

                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Calendar classCal = Calendar.getInstance();
                classCal.setTime(timeFormat.parse(startTime));

                int hour = classCal.get(Calendar.HOUR_OF_DAY);
                int minute = classCal.get(Calendar.MINUTE);

                int dayOfWeek = getDayOfWeekFromName(dayName);
                if (dayOfWeek == -1) continue;

                Calendar today = Calendar.getInstance();
                Calendar nextClass = (Calendar) today.clone();
                nextClass.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                nextClass.set(Calendar.HOUR_OF_DAY, hour);
                nextClass.set(Calendar.MINUTE, minute);
                nextClass.set(Calendar.SECOND, 0);
                nextClass.set(Calendar.MILLISECOND, 0);

                if (nextClass.before(today)) {
                    nextClass.add(Calendar.WEEK_OF_YEAR, 1);
                }

                long triggerTime = System.currentTimeMillis() + (30 * 1000); // Fires in 30 seconds

                Intent intent = new Intent(this, AlarmReceiver.class);
                intent.putExtra("subject", subject);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        requestCode++,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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