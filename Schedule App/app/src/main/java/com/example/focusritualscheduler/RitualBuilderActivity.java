package com.example.focusritualscheduler;

import android.os.Bundle;
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

public class RitualBuilderActivity extends AppCompatActivity {

    private EditText etRitualStep;
    private Button btnAddStep;
    private ListView lvRitualSteps;

    public static ArrayList<String> ritualSteps = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static final String RITUAL_FILENAME = "ritual.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ritual_builder);

        etRitualStep = findViewById(R.id.et_ritual_step);
        btnAddStep = findViewById(R.id.btn_add_step);
        lvRitualSteps = findViewById(R.id.lv_ritual_steps);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, ritualSteps);
        lvRitualSteps.setAdapter(adapter);

        btnAddStep.setOnClickListener(v -> addStep());

        lvRitualSteps.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Step")
                    .setMessage("Delete this ritual step?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        ritualSteps.remove(position);
                        adapter.notifyDataSetChanged();
                        saveRitualToFile();
                        Toast.makeText(this, "Step deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        // Fade-in
        findViewById(R.id.content_layout).setAlpha(0f);
        findViewById(R.id.content_layout).animate().alpha(1f).setDuration(800).start();

        // Load saved ritual
        loadRitualFromFile();
        adapter.notifyDataSetChanged();
    }

    private void addStep() {
        String step = etRitualStep.getText().toString().trim();
        if (step.isEmpty()) {
            Toast.makeText(this, "Please enter a ritual step", Toast.LENGTH_SHORT).show();
            return;
        }

        ritualSteps.add(step);
        adapter.notifyDataSetChanged();
        etRitualStep.setText("");
        Toast.makeText(this, "Step added!", Toast.LENGTH_SHORT).show();

        saveRitualToFile();
    }

    private void saveRitualToFile() {
        try {
            FileOutputStream fos = openFileOutput(RITUAL_FILENAME, MODE_PRIVATE);
            for (String step : ritualSteps) {
                fos.write((step + "\n").getBytes());
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRitualFromFile() {
        ritualSteps.clear();
        try {
            FileInputStream fis = openFileInput(RITUAL_FILENAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    ritualSteps.add(line.trim());
                }
            }
            reader.close();
        } catch (Exception e) {
            // No file yet — normal
        }
    }
}