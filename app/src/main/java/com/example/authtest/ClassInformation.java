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
import com.example.authtest.databinding.StudentsAttendanceStatusCardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class ClassInformation extends AppCompatActivity {

    private ActivityClassInformationBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isSessionActive = false;

    private AppCompatButton addStudentsButton;
    private boolean isStudent = false;
    private String classId;
    private ListenerRegistration attendanceListener; // For real-time updates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassInformationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.backButton.setOnClickListener(v -> finish());

        addStudentsButton = findViewById(R.id.addStudentsButton);

        checkUserTypeAndSetupUI();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CLASS_MODEL")) {
            ClassModel classModel = (ClassModel) intent.getSerializableExtra("CLASS_MODEL");

            if (classModel != null) {
                classId = classModel.getId();

                if (!isStudent) {
                    setupTeacherView(classModel);
                } else {
                    setupStudentView(classModel);
                }

                setupClassInfo(classModel);

            } else {
                Toast.makeText(this, "Class data could not be loaded.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No class data was provided.", Toast.LENGTH_SHORT).show();
            finish();
        }

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

    private void setupTeacherView(ClassModel classModel) {
        AttendanceCardBinding attendanceBinding = binding.attendanceCard;
        attendanceBinding.classCodeText.setText(classModel.getClassCode() != null ? classModel.getClassCode() : "N/A");

        // Set initial state from class model
        isSessionActive = classModel.isAttendanceActive();
        updateTeacherAttendanceUI(attendanceBinding);

        attendanceBinding.attendanceButton.setOnClickListener(v -> {
            isSessionActive = !isSessionActive;
            updateTeacherAttendanceUI(attendanceBinding);
            updateAttendanceStatusInFirestore(isSessionActive);
        });
    }

    private void setupStudentView(ClassModel classModel) {
        // Set up real-time listener for attendance status changes
        if (classId != null && !classId.isEmpty()) {
            DocumentReference classRef = db.collection("allClasses").document(classId);

            attendanceListener = classRef.addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.e("ClassInformation", "Error listening to attendance status", error);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Boolean isActive = snapshot.getBoolean("attendanceActive");
                    if (isActive != null) {
                        updateStudentAttendanceCard(isActive);
                    }
                }
            });
        }
    }

    private void updateTeacherAttendanceUI(AttendanceCardBinding attendanceBinding) {
        if (isSessionActive) {
            attendanceBinding.attendanceButton.setText("End Attendance Session");
            attendanceBinding.attendanceButton.setBackgroundResource(R.drawable.alt_attendance_button);
            attendanceBinding.bellIcon.setBackgroundResource(R.drawable.alt_attendance_button);
            attendanceBinding.classCodeCard.setCardBackgroundColor(0xFFFF5252);
        } else {
            attendanceBinding.attendanceButton.setText("Start Attendance Session");
            attendanceBinding.attendanceButton.setBackgroundResource(R.drawable.attendance_button);
            attendanceBinding.bellIcon.setBackgroundResource(R.drawable.attendance_button);
            attendanceBinding.classCodeCard.setCardBackgroundColor(0xFF2EAD00);
        }
    }

    private void updateStudentAttendanceCard(boolean isActive) {
        StudentsAttendanceStatusCardBinding studentCard = binding.studentAttendanceCard;

        if (isActive) {
            // Session is ACTIVE - show blue "Mark Attendance" state
            studentCard.getRoot().setCardBackgroundColor(0xFF4C6EF5); // Blue
            studentCard.clockIcon.setText("🔔");
            studentCard.clockIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF364FC7));
            studentCard.sessionStatusTitle.setText("Session Active");
            studentCard.sessionStatusTitle.setTextColor(0xFFFFFFFF); // White text
            studentCard.sessionStatusDescription.setText("Started a few seconds ago");
            studentCard.sessionStatusDescription.setTextColor(0xFFFFFFFF); // White text

            // Show Mark Attendance button if it exists
            if (studentCard.markAttendanceButton != null) {
                studentCard.markAttendanceButton.setVisibility(View.VISIBLE);
            }
        } else {
            // Session is INACTIVE - show default gray state
            studentCard.getRoot().setCardBackgroundColor(0xFFD9D9D9); // Gray
            studentCard.clockIcon.setText("🕐");
            studentCard.clockIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFC6C6C7));
            studentCard.sessionStatusTitle.setText("No Active Session");
            studentCard.sessionStatusTitle.setTextColor(0xFF000000); // Black text
            studentCard.sessionStatusDescription.setText("Wait for your teacher to start an attendance session");
            studentCard.sessionStatusDescription.setTextColor(0xFF828282); // Gray text

            // Hide Mark Attendance button if it exists
            if (studentCard.markAttendanceButton != null) {
                studentCard.markAttendanceButton.setVisibility(View.GONE);
            }
        }
    }

    private void updateAttendanceStatusInFirestore(boolean isActive) {
        if (classId == null || classId.isEmpty()) {
            Toast.makeText(this, "Error: Class ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = mAuth.getCurrentUser().getUid();

        // Update in teacher's classes collection
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("attendanceActive", isActive)
                .addOnSuccessListener(unused -> {
                    // Also update in global allClasses collection
                    db.collection("allClasses")
                            .document(classId)
                            .update("attendanceActive", isActive)
                            .addOnSuccessListener(unused2 -> {
                                String message = isActive ? "Attendance session started!" : "Attendance session ended!";
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ClassInformation", "Failed to update allClasses", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ClassInformation", "Failed to update attendance status", e);
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClassInfo(ClassModel classModel) {
        binding.classInfoCard.infoClassName.setText(classModel.getClassName() != null ? classModel.getClassName() : "N/A");
        binding.classInfoCard.infoClassCode.setText(classModel.getClassCode() != null ? classModel.getClassCode() : "N/A");
        binding.classInfoCard.infoSubjectCode.setText(classModel.getSubjectCode() != null ? classModel.getSubjectCode() : "N/A");
        binding.classInfoCard.infoStartTime.setText(classModel.getStartTime() != null ? classModel.getStartTime() : "N/A");
        binding.classInfoCard.infoEndTime.setText(classModel.getEndTime() != null ? classModel.getEndTime() : "N/A");
        binding.classInfoCard.infoRoom.setText(classModel.getRoom() != null ? classModel.getRoom() : "N/A");
        binding.classInfoCard.infoStudents.setText(String.valueOf(classModel.getStudents()));

        String classDays = classModel.getClassDays();
        if (classDays != null && !classDays.isEmpty()) {
            binding.classInfoCard.infoClassDays.setText(classDays);
        } else {
            binding.classInfoCard.infoClassDays.setText("N/A");
        }

        String teacherName = classModel.getTeacher();
        binding.classInfoCard.infoTeacher.setText(teacherName != null && !teacherName.isEmpty() ? teacherName : "N/A");
    }

    private void checkUserTypeAndSetupUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();

            if (email != null && email.contains("@students.")) {
                isStudent = true;
                binding.attendanceCard.getRoot().setVisibility(View.GONE);
                binding.studentAttendanceCard.getRoot().setVisibility(View.VISIBLE);
                if (addStudentsButton != null) {
                    addStudentsButton.setVisibility(View.GONE);
                }
                Log.d("ClassInformation", "Student user detected - showing student attendance card");
            } else {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the real-time listener when activity is destroyed
        if (attendanceListener != null) {
            attendanceListener.remove();
        }
    }
}