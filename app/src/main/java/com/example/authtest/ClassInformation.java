package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ClassInformation extends AppCompatActivity {

    private ActivityClassInformationBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isSessionActive = false;

    private AppCompatButton addStudentsButton;
    private boolean isStudent = false;
    private String classId;
    private ListenerRegistration attendanceListener;
    private ListenerRegistration statsListener;
    private ListenerRegistration classInfoListener;
    private boolean hasMarkedAttendance = false;
    private String attendanceTimestamp = "";

    private TextView totalStudentsCount;
    private TextView presentCount;
    private TextView lateCount;
    private TextView absentCount;

    private String classStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassInformationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.backButton.setOnClickListener(v -> finish());

        addStudentsButton = findViewById(R.id.addStudentsButton);

        // Initialize stats TextViews
        View statsCard = binding.attendanceStatsCard.getRoot();
        totalStudentsCount = statsCard.findViewById(R.id.totalStudentsCount);
        presentCount = statsCard.findViewById(R.id.presentCount);
        lateCount = statsCard.findViewById(R.id.lateCount);
        absentCount = statsCard.findViewById(R.id.absentCount);

        checkUserTypeAndSetupUI();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CLASS_MODEL")) {
            ClassModel classModel = (ClassModel) intent.getSerializableExtra("CLASS_MODEL");

            if (classModel != null) {
                classId = classModel.getId();
                classStartTime = classModel.getStartTime();

                if (!isStudent) {
                    setupTeacherView(classModel);
                    setupAttendanceStatsListener();
                    setupClassInfoListener();
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

        isSessionActive = classModel.isAttendanceActive();
        updateTeacherAttendanceUI(attendanceBinding);

        attendanceBinding.attendanceButton.setOnClickListener(v -> {
            isSessionActive = !isSessionActive;
            updateTeacherAttendanceUI(attendanceBinding);
            updateAttendanceStatusInFirestore(isSessionActive);
        });
    }

    private void setupStudentView(ClassModel classModel) {
        StudentsAttendanceStatusCardBinding studentCard = binding.studentAttendanceCard;

        studentCard.markAttendanceButton.setOnClickListener(v -> {
            markAttendance();
        });

        checkExistingAttendance();

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
                        if (!isActive) {
                            hasMarkedAttendance = false;
                            attendanceTimestamp = "";
                        }
                        updateStudentAttendanceCard(isActive);
                    }
                }
            });
        }
    }

    private void setupAttendanceStatsListener() {
        if (classId == null || classId.isEmpty()) return;

        statsListener = db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("ClassInformation", "Error listening to attendance stats", error);
                        return;
                    }

                    if (snapshots != null) {
                        calculateAttendanceStats(snapshots.getDocuments());
                    }
                });
    }

    private void setupClassInfoListener() {
        if (classId == null || classId.isEmpty()) return;

        classInfoListener = db.collection("allClasses")
                .document(classId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e("ClassInformation", "Error listening to class info", error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Long studentCount = snapshot.getLong("students");
                        if (studentCount != null) {
                            binding.classInfoCard.infoStudents.setText(String.valueOf(studentCount));
                        }
                    }
                });
    }

    private void calculateAttendanceStats(java.util.List<com.google.firebase.firestore.DocumentSnapshot> documents) {
        int total = documents.size();
        int present = 0;
        int late = 0;
        int absent = 0;

        for (var doc : documents) {
            Long timestamp = doc.getLong("timestamp");
            if (timestamp != null) {
                String status = getAttendanceStatus(timestamp);
                switch (status) {
                    case "Present":
                        present++;
                        break;
                    case "Late":
                        late++;
                        break;
                    case "Absent":
                        absent++;
                        break;
                }
            }
        }

        // Update UI
        totalStudentsCount.setText(String.valueOf(total));
        presentCount.setText(String.valueOf(present));
        lateCount.setText(String.valueOf(late));
        absentCount.setText(String.valueOf(absent));
    }

    private String getAttendanceStatus(long markedTimestamp) {
        if (classStartTime == null || classStartTime.isEmpty()) {
            return "Present";
        }

        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date startTime = timeFormat.parse(classStartTime);
            Date markedTime = new Date(markedTimestamp);

            // Get current date and set the time to class start time
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dateFormat.format(markedTime);
            String startTimeString = today + " " + classStartTime;

            SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault());
            Date classStartDateTime = fullFormat.parse(startTimeString);

            if (classStartDateTime == null) return "Present";

            long differenceMs = markedTimestamp - classStartDateTime.getTime();
            long differenceMinutes = differenceMs / (60 * 1000);

            if (differenceMinutes <= 15) {
                return "Present";
            } else if (differenceMinutes <= 30) {
                return "Late";
            } else {
                return "Absent";
            }

        } catch (ParseException e) {
            Log.e("ClassInformation", "Error parsing time", e);
            return "Present";
        }
    }

    private void checkExistingAttendance() {
        if (classId == null || classId.isEmpty()) return;

        String studentId = mAuth.getCurrentUser().getUid();

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean marked = documentSnapshot.getBoolean("marked");
                        Long timestamp = documentSnapshot.getLong("timestamp");

                        if (marked != null && marked && timestamp != null) {
                            hasMarkedAttendance = true;
                            attendanceTimestamp = formatTimestamp(timestamp);

                            db.collection("allClasses")
                                    .document(classId)
                                    .get()
                                    .addOnSuccessListener(classDoc -> {
                                        Boolean isActive = classDoc.getBoolean("attendanceActive");
                                        if (isActive != null && isActive) {
                                            showAttendanceMarkedState();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ClassInformation", "Error checking attendance", e);
                });
    }

    private void markAttendance() {
        if (classId == null || classId.isEmpty()) {
            Toast.makeText(this, "Error: Class ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String studentId = mAuth.getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> attendanceRecord = new HashMap<>();
        attendanceRecord.put("studentId", studentId);
        attendanceRecord.put("classId", classId);
        attendanceRecord.put("timestamp", timestamp);
        attendanceRecord.put("marked", true);

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .document(studentId)
                .set(attendanceRecord)
                .addOnSuccessListener(unused -> {
                    hasMarkedAttendance = true;
                    attendanceTimestamp = formatTimestamp(timestamp);
                    showAttendanceMarkedState();
                    Log.d("ClassInformation", "Attendance marked successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("ClassInformation", "Error marking attendance", e);
                    Toast.makeText(this, "Failed to mark attendance", Toast.LENGTH_SHORT).show();
                });
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return "Recorded at " + sdf.format(new Date(timestamp));
    }

    private void showAttendanceMarkedState() {
        StudentsAttendanceStatusCardBinding studentCard = binding.studentAttendanceCard;

        studentCard.getRoot().setCardBackgroundColor(0xFF51CF66);
        studentCard.clockIcon.setText("✓");
        studentCard.clockIcon.setTextSize(56);
        studentCard.clockIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2F9E44));
        studentCard.sessionStatusTitle.setText("Attendance Marked!");
        studentCard.sessionStatusTitle.setTextColor(0xFFFFFFFF);
        studentCard.sessionStatusDescription.setText(attendanceTimestamp);
        studentCard.sessionStatusDescription.setTextColor(0xFFFFFFFF);
        studentCard.markAttendanceButton.setVisibility(View.GONE);
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
        if (hasMarkedAttendance && isActive) {
            showAttendanceMarkedState();
            return;
        }

        StudentsAttendanceStatusCardBinding studentCard = binding.studentAttendanceCard;

        if (isActive) {
            studentCard.getRoot().setCardBackgroundColor(0xFF4C6EF5);
            studentCard.clockIcon.setText("🔔");
            studentCard.clockIcon.setTextSize(48);
            studentCard.clockIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF364FC7));
            studentCard.sessionStatusTitle.setText("Session Active");
            studentCard.sessionStatusTitle.setTextColor(0xFFFFFFFF);
            studentCard.sessionStatusDescription.setText("Started a few seconds ago");
            studentCard.sessionStatusDescription.setTextColor(0xFFFFFFFF);
            studentCard.markAttendanceButton.setVisibility(View.VISIBLE);
        } else {
            hasMarkedAttendance = false;
            attendanceTimestamp = "";

            if (classId != null) {
                String studentId = mAuth.getCurrentUser().getUid();
                db.collection("allClasses")
                        .document(classId)
                        .collection("attendanceRecords")
                        .document(studentId)
                        .delete();
            }

            studentCard.getRoot().setCardBackgroundColor(0xFFD9D9D9);
            studentCard.clockIcon.setText("🕐");
            studentCard.clockIcon.setTextSize(48);
            studentCard.clockIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFC6C6C7));
            studentCard.sessionStatusTitle.setText("No Active Session");
            studentCard.sessionStatusTitle.setTextColor(0xFF000000);
            studentCard.sessionStatusDescription.setText("Wait for your teacher to start an attendance session");
            studentCard.sessionStatusDescription.setTextColor(0xFF828282);
            studentCard.markAttendanceButton.setVisibility(View.GONE);
        }
    }

    private void updateAttendanceStatusInFirestore(boolean isActive) {
        if (classId == null || classId.isEmpty()) {
            Toast.makeText(this, "Error: Class ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("attendanceActive", isActive)
                .addOnSuccessListener(unused -> {
                    db.collection("allClasses")
                            .document(classId)
                            .update("attendanceActive", isActive)
                            .addOnSuccessListener(unused2 -> {
                                String message = isActive ? "Attendance session started!" : "Attendance session ended!";
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                                if (!isActive) {
                                    clearAllAttendanceRecords();
                                }
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

    private void clearAllAttendanceRecords() {
        if (classId == null || classId.isEmpty()) return;

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                    Log.d("ClassInformation", "All attendance records cleared");
                })
                .addOnFailureListener(e -> {
                    Log.e("ClassInformation", "Error clearing attendance records", e);
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
                binding.attendanceStatsCard.getRoot().setVisibility(View.GONE);
                if (addStudentsButton != null) {
                    addStudentsButton.setVisibility(View.GONE);
                }
                Log.d("ClassInformation", "Student user detected - showing student attendance card");
            } else {
                isStudent = false;
                binding.attendanceCard.getRoot().setVisibility(View.VISIBLE);
                binding.studentAttendanceCard.getRoot().setVisibility(View.GONE);
                binding.attendanceStatsCard.getRoot().setVisibility(View.VISIBLE);
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
        if (attendanceListener != null) {
            attendanceListener.remove();
        }
        if (statsListener != null) {
            statsListener.remove();
        }
        if (classInfoListener != null) {
            classInfoListener.remove();
        }
    }
}