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

    private void addStudentToClass() {
        String studentEmail = studentEmailInput.getText().toString().trim().toLowerCase();

        // Validate email
        if (studentEmail.isEmpty()) {
            studentEmailInput.setError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(studentEmail).matches()) {
            studentEmailInput.setError("Please enter a valid email");
            return;
        }

        // Check if it's a student email
        if (!studentEmail.contains("@students.")) {
            studentEmailInput.setError("Must be a student email (@students.)");
            return;
        }

        // Disable button to prevent multiple clicks
        addStudentButton.setEnabled(false);
        addStudentButton.setText("Adding...");

        String teacherId = mAuth.getCurrentUser().getUid();

        // Add email to both teacher's class and global allClasses
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("allowedStudentEmails", FieldValue.arrayUnion(studentEmail))
                .addOnSuccessListener(unused -> {
                    // Also update in allClasses collection
                    db.collection("allClasses")
                            .document(classId)
                            .update("allowedStudentEmails", FieldValue.arrayUnion(studentEmail))
                            .addOnSuccessListener(unused2 -> {
                                Log.d(TAG, "Student email added successfully: " + studentEmail);
                                Toast.makeText(this, "Student added successfully!", Toast.LENGTH_SHORT).show();
                                studentEmailInput.setText("");
                                addStudentButton.setEnabled(true);
                                addStudentButton.setText("Add Student");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update allClasses", e);
                                Toast.makeText(this, "Added to class but failed to sync globally", Toast.LENGTH_SHORT).show();
                                addStudentButton.setEnabled(true);
                                addStudentButton.setText("Add Student");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add student", e);
                    Toast.makeText(this, "Failed to add student: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    addStudentButton.setEnabled(true);
                    addStudentButton.setText("Add Student");
                });
    }
}