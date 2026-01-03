package com.example.authtest;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * FILE: AddStudentsForm.java
 * LOCATION: src/main/java/com/example/authtest/AddStudentsForm.java
 *
 * PURPOSE: Activity for adding students to a class with duplicate prevention
 *
 * FEATURES:
 * - Email validation
 * - Student email format verification (@students.)
 * - Duplicate detection (case-insensitive)
 * - Firestore integration
 * - Student count tracking
 */
public class AddStudentsForm extends AppCompatActivity {

    private static final String TAG = "AddStudentsForm";
    private AppCompatButton backButton;
    private AppCompatButton addStudentButton;
    private EditText studentEmailInput;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String classId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_students_form);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get class ID from intent
        classId = getIntent().getStringExtra("CLASS_ID");

        if (classId == null || classId.isEmpty()) {
            Toast.makeText(this, "Error: Class ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton = findViewById(R.id.backButton);
        addStudentButton = findViewById(R.id.addStudentToClassButton);
        studentEmailInput = findViewById(R.id.studentEmailInput);

        backButton.setOnClickListener(v -> finish());

        addStudentButton.setOnClickListener(v -> addStudentToClass());
    }

    /**
     * Step 1: Validate email and initiate duplicate check
     */
    private void addStudentToClass() {
        String studentEmail = studentEmailInput.getText().toString().trim().toLowerCase();

        // Validate email is not empty
        if (studentEmail.isEmpty()) {
            studentEmailInput.setError("Email is required");
            return;
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(studentEmail).matches()) {
            studentEmailInput.setError("Please enter a valid email");
            return;
        }

        // Validate it's a student email (@students.)
        if (!studentEmail.contains("@students.")) {
            studentEmailInput.setError("Must be a student email (@students.)");
            return;
        }

        // Disable button to prevent multiple clicks
        addStudentButton.setEnabled(false);
        addStudentButton.setText("Checking...");

        String teacherId = mAuth.getCurrentUser().getUid();

        // Check if student already exists in the class
        checkIfStudentExists(studentEmail, teacherId);
    }

    /**
     * Step 2: Check if student email already exists in class
     * This prevents duplicate student entries
     */
    private void checkIfStudentExists(String studentEmail, String teacherId) {
        db.collection("allClasses")
                .document(classId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        List<String> allowedEmails = (List<String>) document.get("allowedStudentEmails");

                        // Check if student email already exists in the class
                        if (allowedEmails != null && allowedEmails.size() > 0) {
                            for (String email : allowedEmails) {
                                // Case-insensitive comparison
                                if (email != null && email.equalsIgnoreCase(studentEmail)) {
                                    // Student already exists - show error
                                    Log.d(TAG, "Student already exists in class: " + studentEmail);
                                    studentEmailInput.setError("This student is already added to the class");
                                    Toast.makeText(
                                            AddStudentsForm.this,
                                            "Student already exists in this class",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    addStudentButton.setEnabled(true);
                                    addStudentButton.setText("Add Student");
                                    return;
                                }
                            }
                        }

                        // Student doesn't exist, safe to proceed with adding
                        Log.d(TAG, "Student is new, proceeding with addition");
                        addStudentToClassFirebase(studentEmail, teacherId);
                    } else {
                        Log.e(TAG, "Class document does not exist");
                        Toast.makeText(AddStudentsForm.this, "Error: Class not found", Toast.LENGTH_SHORT).show();
                        addStudentButton.setEnabled(true);
                        addStudentButton.setText("Add Student");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check if student exists", e);
                    Toast.makeText(
                            AddStudentsForm.this,
                            "Error checking student: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                    addStudentButton.setEnabled(true);
                    addStudentButton.setText("Add Student");
                });
    }

    /**
     * Step 3: Add student to class in Firestore
     * Only called after duplicate check passes
     */
    private void addStudentToClassFirebase(String studentEmail, String teacherId) {
        addStudentButton.setText("Adding...");

        // Add email to teacher's class collection
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("allowedStudentEmails", FieldValue.arrayUnion(studentEmail))
                .addOnSuccessListener(unused -> {
                    // Also update in global allClasses collection
                    db.collection("allClasses")
                            .document(classId)
                            .update("allowedStudentEmails", FieldValue.arrayUnion(studentEmail))
                            .addOnSuccessListener(unused2 -> {
                                // Increment the student count
                                incrementStudentCount(teacherId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update allClasses", e);
                                Toast.makeText(
                                        this,
                                        "Added to class but failed to sync globally",
                                        Toast.LENGTH_SHORT
                                ).show();
                                addStudentButton.setEnabled(true);
                                addStudentButton.setText("Add Student");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add student", e);
                    Toast.makeText(
                            this,
                            "Failed to add student: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                    addStudentButton.setEnabled(true);
                    addStudentButton.setText("Add Student");
                });
    }

    /**
     * Step 4: Increment student count in both teacher's class and allClasses
     */
    private void incrementStudentCount(String teacherId) {
        // Increment in teacher's class collection
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("students", FieldValue.increment(1))
                .addOnSuccessListener(unused -> {
                    // Also increment in global allClasses collection
                    db.collection("allClasses")
                            .document(classId)
                            .update("students", FieldValue.increment(1))
                            .addOnSuccessListener(unused2 -> {
                                Log.d(TAG, "Student count incremented successfully");
                                Toast.makeText(this, "Student added successfully!", Toast.LENGTH_SHORT).show();
                                studentEmailInput.setText("");
                                addStudentButton.setEnabled(true);
                                addStudentButton.setText("Add Student");

                                // Notify ClassInformation to refresh the student list
                                setResult(RESULT_OK);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to increment count in allClasses", e);
                                Toast.makeText(
                                        this,
                                        "Student added but count not updated",
                                        Toast.LENGTH_SHORT
                                ).show();
                                addStudentButton.setEnabled(true);
                                addStudentButton.setText("Add Student");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to increment student count", e);
                    Toast.makeText(
                            this,
                            "Student added but count not updated",
                            Toast.LENGTH_SHORT
                    ).show();
                    addStudentButton.setEnabled(true);
                    addStudentButton.setText("Add Student");
                });
    }
}