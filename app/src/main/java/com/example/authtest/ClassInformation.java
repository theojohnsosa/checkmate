package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.authtest.databinding.ActivityClassInformationBinding;
import com.example.authtest.databinding.AttendanceCardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ClassInformation extends AppCompatActivity {

    private ActivityClassInformationBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isSessionActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassInformationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Back button
        binding.backButton.setOnClickListener(v -> finish());

        // Load teacher name
        loadCurrentUserName();

        // Get intent data
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CLASS_MODEL")) {
            ClassModel classModel = (ClassModel) intent.getSerializableExtra("CLASS_MODEL");

            if (classModel != null) {

                // --- Attendance Card Setup ---
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
                List<String> days = classModel.getDays();
                if (days != null && !days.isEmpty()) {
                    StringBuilder daysBuilder = new StringBuilder();
                    for (int i = 0; i < days.size(); i++) {
                        String day = days.get(i);
                        if (day != null && !day.isEmpty()) {
                            daysBuilder.append(day.substring(0, Math.min(day.length(), 3)));
                            if (i < days.size() - 1) daysBuilder.append("/");
                        }
                    }
                    binding.classInfoCard.infoClassDays.setText(daysBuilder.toString());
                } else {
                    binding.classInfoCard.infoClassDays.setText("N/A");
                }

            } else {
                Toast.makeText(this, "Class data could not be loaded.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No class data was provided.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadCurrentUserName() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");
                            String fullName = (firstName != null && lastName != null)
                                    ? firstName + " " + lastName
                                    : currentUser.getDisplayName();
                            binding.classInfoCard.infoTeacher.setText(fullName != null ? fullName : "N/A");
                        } else if (currentUser.getDisplayName() != null) {
                            binding.classInfoCard.infoTeacher.setText(currentUser.getDisplayName());
                        } else {
                            binding.classInfoCard.infoTeacher.setText("N/A");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ClassInformation", "Error loading user name", e);
                        if (currentUser.getDisplayName() != null) {
                            binding.classInfoCard.infoTeacher.setText(currentUser.getDisplayName());
                        } else {
                            binding.classInfoCard.infoTeacher.setText("N/A");
                        }
                    });
        } else {
            binding.classInfoCard.infoTeacher.setText("N/A");
        }
    }
}