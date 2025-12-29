package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.authtest.databinding.ActivityClassInformationBinding;
import com.example.authtest.databinding.AttendanceCardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ClassInformation extends AppCompatActivity {

    private ActivityClassInformationBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isSessionActive = false;

    private AppCompatButton addStudentsButton;
    private boolean isStudent = false;
    private String classId; // Store class ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassInformationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Back button
        binding.backButton.setOnClickListener(v -> finish());

        // Initialize addStudentsButton
        addStudentsButton = findViewById(R.id.addStudentsButton);

        // Check if user is student or teacher
        checkUserTypeAndSetupUI();

        // Get intent data
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CLASS_MODEL")) {
            ClassModel classModel = (ClassModel) intent.getSerializableExtra("CLASS_MODEL");

            if (classModel != null) {
                // Store class ID for later use
                classId = classModel.getId();

                // --- Attendance Card Setup (only for teachers) ---
                AttendanceCardBinding attendanceBinding = binding.attendanceCard;
                attendanceBinding.classCodeText.setText(classModel.getClassCode() != null ? classModel.getClassCode() : "N/A");

                attendanceBinding.attendanceButton.setOnClickListener(v -> {
                    isSessionActive = !isSessionActive;
                    if (isSessionActive) {
                        attendanceBinding.attendanceButton.setText("End Attendance Session");
                        attendanceBinding.attendanceButton.setBackgroundResource(R.drawable.alt_attendance_button);
                        attendanceBinding.bellIcon.setBackgroundResource(R.drawable.alt_attendance_button);
                        attendanceBinding.classCodeCard.setCardBackgroundColor(0xFFFF5252); // Red
                        Toast.makeText(this, "Attendance session started!", Toast.LENGTH_SHORT).show();
                    } else {
                        attendanceBinding.attendanceButton.setText("Start Attendance Session");
                        attendanceBinding.attendanceButton.setBackgroundResource(R.drawable.attendance_button);
                        attendanceBinding.bellIcon.setBackgroundResource(R.drawable.attendance_button);
                        attendanceBinding.classCodeCard.setCardBackgroundColor(0xFF2EAD00); // Green
                        Toast.makeText(this, "Attendance session ended!", Toast.LENGTH_SHORT).show();
                    }
                });

                // --- Class Info Card Setup ---
                binding.classInfoCard.infoClassName.setText(classModel.getClassName() != null ? classModel.getClassName() : "N/A");
                binding.classInfoCard.infoClassCode.setText(classModel.getClassCode() != null ? classModel.getClassCode() : "N/A");
                binding.classInfoCard.infoSubjectCode.setText(classModel.getSubjectCode() != null ? classModel.getSubjectCode() : "N/A");
                binding.classInfoCard.infoStartTime.setText(classModel.getStartTime() != null ? classModel.getStartTime() : "N/A");
                binding.classInfoCard.infoEndTime.setText(classModel.getEndTime() != null ? classModel.getEndTime() : "N/A");
                binding.classInfoCard.infoRoom.setText(classModel.getRoom() != null ? classModel.getRoom() : "N/A");
                binding.classInfoCard.infoStudents.setText(String.valueOf(classModel.getStudents()));

                // Format class days
                String classDays = classModel.getClassDays();
                if (classDays != null && !classDays.isEmpty()) {
                    binding.classInfoCard.infoClassDays.setText(classDays);
                } else {
                    binding.classInfoCard.infoClassDays.setText("N/A");
                }

                // Set teacher name from ClassModel (not from current user!)
                String teacherName = classModel.getTeacher();
                binding.classInfoCard.infoTeacher.setText(teacherName != null && !teacherName.isEmpty() ? teacherName : "N/A");

            } else {
                Toast.makeText(this, "Class data could not be loaded.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No class data was provided.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Setup Add Students button click listener (only if visible for teachers)
        if (addStudentsButton != null && !isStudent) {
            addStudentsButton.setOnClickListener(v -> {
                if (classId != null && !classId.isEmpty()) {
                    Intent addStudentIntent = new Intent(ClassInformation.this, AddStudentsForm.class);
                    addStudentIntent.putExtra("CLASS_ID", classId);
                    startActivity(addStudentIntent);
                } else {
                    Toast.makeText(this, "Error: Class ID not found", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkUserTypeAndSetupUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();

            // Check if user is a student (email contains "@students.")
            if (email != null && email.contains("@students.")) {
                // User is a STUDENT - show student attendance card, hide teacher features
                isStudent = true;
                binding.attendanceCard.getRoot().setVisibility(View.GONE);
                binding.studentAttendanceCard.getRoot().setVisibility(View.VISIBLE);
                if (addStudentsButton != null) {
                    addStudentsButton.setVisibility(View.GONE);
                }
                Log.d("ClassInformation", "Student user detected - showing student attendance card");
            } else {
                // User is a TEACHER - show teacher attendance card, hide student card
                isStudent = false;
                binding.attendanceCard.getRoot().setVisibility(View.VISIBLE);
                binding.studentAttendanceCard.getRoot().setVisibility(View.GONE);
                if (addStudentsButton != null) {
                    addStudentsButton.setVisibility(View.VISIBLE);
                }
                Log.d("ClassInformation", "Teacher user detected - showing all features");
            }
        }
    }
}