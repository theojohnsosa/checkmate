package com.example.authtest;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CreateClass extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private EditText classNameInput;
    private EditText subjectCodeInput;
    private ToggleButton monToggle, tueToggle, wedToggle, thuToggle, friToggle, satToggle;
    private AutoCompleteTextView startTimeInput;
    private AutoCompleteTextView endTimeInput;
    private EditText roomInput;
    private AppCompatButton createClassButton;
    private AppCompatButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        classNameInput = findViewById(R.id.classNameInput);
        subjectCodeInput = findViewById(R.id.subjectCodeInput);
        monToggle = findViewById(R.id.monToggle);
        tueToggle = findViewById(R.id.tueToggle);
        wedToggle = findViewById(R.id.wedToggle);
        thuToggle = findViewById(R.id.thuToggle);
        friToggle = findViewById(R.id.friToggle);
        satToggle = findViewById(R.id.satToggle);
        startTimeInput = findViewById(R.id.startTimeInput);
        endTimeInput = findViewById(R.id.endTimeInput);
        roomInput = findViewById(R.id.roomInput);
        createClassButton = findViewById(R.id.createClassButton);
        backButton = findViewById(R.id.backButton);

        // Setup time dropdowns
        setupTimeDropdowns();

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Create button listener
        createClassButton.setOnClickListener(v -> {
            if (validateInputs()) {
                String classDays = getSelectedDays();

                ClassModel classModel = new ClassModel(
                        classNameInput.getText().toString().trim(),
                        generateClassCode(),
                        subjectCodeInput.getText().toString().trim(),
                        classDays,
                        startTimeInput.getText().toString().trim(),
                        endTimeInput.getText().toString().trim(),
                        roomInput.getText().toString().trim(),
                        mAuth.getCurrentUser().getDisplayName(),
                        0
                );
                createClass(classModel);
            }
        });
    }

    private void setupTimeDropdowns() {
        List<String> timeSlots = new ArrayList<>();

        // Generate time slots from 6:00 AM to 10:00 PM
        String[] periods = {"AM", "PM"};
        for (String period : periods) {
            int startHour = period.equals("AM") ? 6 : 1;
            int endHour = period.equals("AM") ? 12 : 10;

            for (int hour = startHour; hour <= endHour; hour++) {
                timeSlots.add(String.format("%d:00 %s", hour, period));
                timeSlots.add(String.format("%d:30 %s", hour, period));
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                timeSlots
        );

        startTimeInput.setAdapter(adapter);
        endTimeInput.setAdapter(adapter);

        // Show dropdown on click
        startTimeInput.setOnClickListener(v -> startTimeInput.showDropDown());
        endTimeInput.setOnClickListener(v -> endTimeInput.showDropDown());
    }

    private String getSelectedDays() {
        List<String> days = new ArrayList<>();
        if (monToggle.isChecked()) days.add("Monday");
        if (tueToggle.isChecked()) days.add("Tuesday");
        if (wedToggle.isChecked()) days.add("Wednesday");
        if (thuToggle.isChecked()) days.add("Thursday");
        if (friToggle.isChecked()) days.add("Friday");
        if (satToggle.isChecked()) days.add("Saturday");

        return String.join("/", days);
    }

    private String generateClassCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }

    private boolean validateInputs() {
        if (classNameInput.getText().toString().trim().isEmpty()) {
            classNameInput.setError("Required");
            return false;
        }
        if (subjectCodeInput.getText().toString().trim().isEmpty()) {
            subjectCodeInput.setError("Required");
            return false;
        }
        if (getSelectedDays().isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (startTimeInput.getText().toString().trim().isEmpty()) {
            startTimeInput.setError("Required");
            return false;
        }
        if (endTimeInput.getText().toString().trim().isEmpty()) {
            endTimeInput.setError("Required");
            return false;
        }
        if (roomInput.getText().toString().trim().isEmpty()) {
            roomInput.setError("Required");
            return false;
        }
        return true;
    }

    private void createClass(ClassModel classModel) {
        String teacherId = mAuth.getCurrentUser().getUid();

        // First get teacher's name from Firestore
        db.collection("users")
                .document(teacherId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String teacherName = "Unknown";

                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        if (firstName != null && lastName != null) {
                            teacherName = firstName + " " + lastName;
                        } else if (mAuth.getCurrentUser().getDisplayName() != null) {
                            teacherName = mAuth.getCurrentUser().getDisplayName();
                        }
                    } else if (mAuth.getCurrentUser().getDisplayName() != null) {
                        teacherName = mAuth.getCurrentUser().getDisplayName();
                    }

                    // Now create the class with the teacher's name
                    ClassModel newClassModel = new ClassModel(
                            classModel.getClassName(),
                            classModel.getClassCode(),
                            classModel.getSubjectCode(),
                            classModel.getClassDays(),
                            classModel.getStartTime(),
                            classModel.getEndTime(),
                            classModel.getRoom(),
                            teacherName,
                            0
                    );

                    db.collection("users")
                            .document(teacherId)
                            .collection("classes")
                            .add(newClassModel)
                            .addOnSuccessListener(documentReference -> {
                                String classId = documentReference.getId();

                                // Save to global collection for easy searching
                                db.collection("allClasses")
                                        .document(classId)
                                        .set(newClassModel)
                                        .addOnSuccessListener(unused2 -> {
                                            // Update both documents with ID
                                            documentReference.update("id", classId);
                                            db.collection("allClasses").document(classId).update("id", classId);

                                            Toast.makeText(this, "Class created successfully!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Class created but not searchable", Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to create class: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch teacher info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}