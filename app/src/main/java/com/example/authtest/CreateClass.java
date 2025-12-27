package com.example.authtest;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ToggleButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CreateClass extends AppCompatActivity {

    private EditText classNameInput, subjectCodeInput, roomInput;
    private AutoCompleteTextView startTimeInput, endTimeInput;

    private ToggleButton monToggle, tueToggle, wedToggle, thuToggle, friToggle, satToggle;
    private AppCompatButton createClassButton, backButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        classNameInput = findViewById(R.id.classNameInput);
        subjectCodeInput = findViewById(R.id.subjectCodeInput);
        roomInput = findViewById(R.id.roomInput);
        startTimeInput = findViewById(R.id.startTimeInput);
        endTimeInput = findViewById(R.id.endTimeInput);

        monToggle = findViewById(R.id.monToggle);
        tueToggle = findViewById(R.id.tueToggle);
        wedToggle = findViewById(R.id.wedToggle);
        thuToggle = findViewById(R.id.thuToggle);
        friToggle = findViewById(R.id.friToggle);
        satToggle = findViewById(R.id.satToggle);

        createClassButton = findViewById(R.id.createClassButton);
        backButton = findViewById(R.id.backButton);

        setupToggleButtons();
        setupTimePresets();

        backButton.setOnClickListener(v -> finish());

        createClassButton.setOnClickListener(v -> {
            if (validateInputs()) {
                createClass();
            }
        });
    }

    private void setupTimePresets() {
        String[] timeOptions = {
                "7:00 AM", "7:30 AM",
                "8:00 AM", "8:30 AM",
                "9:00 AM", "9:30 AM",
                "10:00 AM", "10:30 AM",
                "11:00 AM", "11:30 AM",
                "12:00 PM", "12:30 PM",
                "1:00 PM", "1:30 PM",
                "2:00 PM", "2:30 PM",
                "3:00 PM", "3:30 PM",
                "4:00 PM", "4:30 PM",
                "5:00 PM", "5:30 PM",
                "6:00 PM", "6:30 PM"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                timeOptions
        );

        startTimeInput.setOnClickListener(v -> {
            startTimeInput.showDropDown();
        });
        startTimeInput.setAdapter(adapter);

        endTimeInput.setOnClickListener(v -> {
            endTimeInput.showDropDown();
        });
        endTimeInput.setAdapter(adapter);
    }

    private void setupToggleButtons() {
        ToggleButton[] toggles = {
                monToggle, tueToggle, wedToggle,
                thuToggle, friToggle, satToggle
        };

        for (ToggleButton toggle : toggles) {
            toggle.setOnCheckedChangeListener(
                    (buttonView, isChecked) ->
                            buttonView.setTextColor(
                                    isChecked ? Color.BLACK : Color.parseColor("#6F6F6F")
                            )
            );
        }
    }

    private boolean validateInputs() {
        if (classNameInput.getText().toString().trim().isEmpty()) {
            classNameInput.setError("Class name required");
            return false;
        }

        if (subjectCodeInput.getText().toString().trim().isEmpty()) {
            subjectCodeInput.setError("Subject code required");
            return false;
        }

        if (!isAnyDaySelected()) {
            Toast.makeText(this, "Select at least one day", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (startTimeInput.getText().toString().trim().isEmpty()) {
            startTimeInput.setError("Start time required");
            return false;
        }

        if (endTimeInput.getText().toString().trim().isEmpty()) {
            endTimeInput.setError("End time required");
            return false;
        }

        if (roomInput.getText().toString().trim().isEmpty()) {
            roomInput.setError("Room required");
            return false;
        }

        return true;
    }

    private boolean isAnyDaySelected() {
        return monToggle.isChecked() || tueToggle.isChecked()
                || wedToggle.isChecked() || thuToggle.isChecked()
                || friToggle.isChecked() || satToggle.isChecked();
    }

    private List<String> getSelectedDays() {
        List<String> days = new ArrayList<>();

        if (monToggle.isChecked()) days.add("Monday");
        if (tueToggle.isChecked()) days.add("Tuesday");
        if (wedToggle.isChecked()) days.add("Wednesday");
        if (thuToggle.isChecked()) days.add("Thursday");
        if (friToggle.isChecked()) days.add("Friday");
        if (satToggle.isChecked()) days.add("Saturday");

        return days;
    }

    private void createClass() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        createClassButton.setEnabled(false);
        createClassButton.setText("Creating Class...");

        String className = classNameInput.getText().toString().trim();
        String subjectCode = subjectCodeInput.getText().toString().trim();
        String room = roomInput.getText().toString().trim();
        String startTime = startTimeInput.getText().toString().trim();
        String endTime = endTimeInput.getText().toString().trim();
        List<String> days = getSelectedDays();
        String userId = currentUser.getUid();

        ClassModel classModel = new ClassModel();
        classModel.setClassName(className);
        classModel.setSubjectCode(subjectCode);
        classModel.setRoom(room);
        classModel.setStartTime(startTime);
        classModel.setEndTime(endTime);
        classModel.setDays(days);
        classModel.setCreatedAt(System.currentTimeMillis());

        db.collection("users")
                .document(userId)
                .collection("classes")
                .add(classModel)
                .addOnSuccessListener(documentReference -> {
                   String classId = documentReference.getId();
                   documentReference.update("id", classId);

                   Toast.makeText(CreateClass.this, "Class Created Successfully", Toast.LENGTH_SHORT).show();
                   finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateClass.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();

                    createClassButton.setEnabled(true);
                    createClassButton.setText("Create Class");
                });

    }
}
