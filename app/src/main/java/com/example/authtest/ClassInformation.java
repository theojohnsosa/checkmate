package com.example.authtest;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.authtest.databinding.ActivityClassInformationBinding;
import com.example.authtest.databinding.AttendanceCardBinding;
import com.example.authtest.databinding.StudentsAttendanceStatusCardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassInformation extends AppCompatActivity {

    private static final String TAG = "ClassInformation";
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
    private ListenerRegistration studentsListener;
    private boolean hasMarkedAttendance = false;
    private String attendanceTimestamp = "";

    private TextView totalStudentsCount;
    private TextView presentCount;
    private TextView lateCount;
    private TextView absentCount;

    private String classStartTime;

    // Student List Components
    private RecyclerView studentsRecyclerView;
    private StudentAttendanceAdapter studentAdapter;
    private List<StudentAttendanceModel> studentList = new ArrayList<>();
    private TextView studentsAttendedHeader;

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

        // Initialize student list views
        studentsRecyclerView = findViewById(R.id.studentsRecyclerView);
        studentsAttendedHeader = findViewById(R.id.studentsAttendedHeader);

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
                    setupStudentListRecyclerView();
                    loadStudentList();
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
                    startActivityForResult(addStudentIntent, 100);
                } else {
                    Toast.makeText(this, "Error: Class ID not found", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Refresh student list when returning from AddStudentsForm
            Log.d(TAG, "Returning from AddStudentsForm, refreshing list");
            loadStudentList();
        }
    }

    private void setupStudentListRecyclerView() {
        Log.d(TAG, "Setting up student list RecyclerView");
        // Setup adapter
        studentAdapter = new StudentAttendanceAdapter(this::removeStudentFromClass);
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentsRecyclerView.setAdapter(studentAdapter);
        studentsRecyclerView.setNestedScrollingEnabled(false);

        setupSwipeToDelete();
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.parseColor("#C92A2A"));
            private final Drawable deleteIcon = ContextCompat.getDrawable(ClassInformation.this, android.R.drawable.ic_menu_delete);

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                StudentAttendanceModel student = studentAdapter.getStudentAt(position);

                // Show confirmation dialog
                new AlertDialog.Builder(ClassInformation.this)
                        .setTitle("Remove Student")
                        .setMessage("Are you sure you want to remove " + student.getFullName() + " from this class?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            removeStudentFromClass(student, position);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            // Restore the item
                            studentAdapter.notifyItemChanged(position);
                        })
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                int backgroundCornerOffset = 20;

                if (dX < 0) { // Swiping to the left
                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                    int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                    background.setBounds(
                            itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                            itemView.getTop(),
                            itemView.getRight(),
                            itemView.getBottom()
                    );

                    background.draw(c);
                    deleteIcon.draw(c);
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(studentsRecyclerView);
    }

    private void loadStudentList() {
        if (classId == null || classId.isEmpty()) {
            Log.e(TAG, "Cannot load students - classId is null or empty");
            return;
        }

        Log.d(TAG, "Loading student list for class: " + classId);

        db.collection("allClasses")
                .document(classId)
                .get()
                .addOnSuccessListener(classDoc -> {
                    if (classDoc.exists()) {
                        List<String> allowedEmails = (List<String>) classDoc.get("allowedStudentEmails");

                        Log.d(TAG, "Found allowedStudentEmails: " + (allowedEmails != null ? allowedEmails.size() : 0));

                        if (allowedEmails == null || allowedEmails.isEmpty()) {
                            Log.d(TAG, "No students in allowedStudentEmails");
                            studentList.clear();
                            studentAdapter.setStudents(studentList);
                            updateStudentsHeaderVisibility();
                            return;
                        }

                        studentList.clear();
                        final int[] loadedCount = {0};
                        final int totalEmails = allowedEmails.size();

                        Log.d(TAG, "Loading " + totalEmails + " students");

                        for (String email : allowedEmails) {
                            String cleanEmail = email.toLowerCase().trim();
                            Log.d(TAG, "Searching for user with email: " + cleanEmail);

                            // Get all users and find the matching one
                            db.collection("users")
                                    .get()
                                    .addOnSuccessListener(allUsersSnapshot -> {
                                        boolean foundUser = false;

                                        for (var userDoc : allUsersSnapshot.getDocuments()) {
                                            String userEmail = userDoc.getString("schoolEmail");

                                            if (userEmail != null && userEmail.toLowerCase().trim().equals(cleanEmail)) {
                                                String firstName = userDoc.getString("firstName");
                                                String lastName = userDoc.getString("lastName");
                                                String studentId = userDoc.getId();

                                                Log.d(TAG, "Found student: " + firstName + " " + lastName + " (" + userEmail + ")");

                                                StudentAttendanceModel student = new StudentAttendanceModel(
                                                        studentId, email,
                                                        firstName != null ? firstName : "Unknown",
                                                        lastName != null ? lastName : "User"
                                                );

                                                // Check if student has marked attendance
                                                checkStudentAttendance(student);
                                                studentList.add(student);
                                                foundUser = true;
                                                break;
                                            }
                                        }

                                        if (!foundUser) {
                                            Log.w(TAG, "No user found for email: " + cleanEmail);
                                            // Still add the student with just email info
                                            StudentAttendanceModel student = new StudentAttendanceModel(
                                                    null, email, "Unknown", "User"
                                            );
                                            student.setAttendanceStatus("Not Marked");
                                            studentList.add(student);
                                        }

                                        loadedCount[0]++;
                                        Log.d(TAG, "Loaded " + loadedCount[0] + " of " + totalEmails + " students");

                                        if (loadedCount[0] == totalEmails) {
                                            Log.d(TAG, "All students loaded, updating UI. Total students: " + studentList.size());
                                            studentAdapter.setStudents(studentList);
                                            updateStudentsHeaderVisibility();
                                            setupStudentsListener();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error loading all users: " + cleanEmail, e);
                                        // Still add the student with just email info
                                        StudentAttendanceModel student = new StudentAttendanceModel(
                                                null, email, "Unknown", "User"
                                        );
                                        student.setAttendanceStatus("Not Marked");
                                        studentList.add(student);

                                        loadedCount[0]++;
                                        if (loadedCount[0] == totalEmails) {
                                            Log.d(TAG, "All students loaded (with errors), updating UI. Total students: " + studentList.size());
                                            studentAdapter.setStudents(studentList);
                                            updateStudentsHeaderVisibility();
                                        }
                                    });
                        }
                    } else {
                        Log.e(TAG, "Class document does not exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading class data", e);
                });
    }

    private void checkStudentAttendance(StudentAttendanceModel student) {
        if (classId == null || student.getStudentId() == null) {
            Log.d(TAG, "Cannot check attendance - classId or studentId is null");
            student.setAttendanceStatus("Not Marked");
            return;
        }

        Log.d(TAG, "Checking attendance for student: " + student.getFullName());

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .document(student.getStudentId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean marked = doc.getBoolean("marked");
                        Long timestamp = doc.getLong("timestamp");

                        Log.d(TAG, "Attendance record found for " + student.getFullName() + ": marked=" + marked + ", timestamp=" + timestamp);

                        if (marked != null && marked && timestamp != null) {
                            student.setMarked(true);
                            student.setTimestamp(timestamp);
                            String status = getAttendanceStatus(timestamp);
                            student.setAttendanceStatus(status);
                            Log.d(TAG, "Student " + student.getFullName() + " attendance status: " + status);
                        } else {
                            student.setAttendanceStatus("Not Marked");
                            Log.d(TAG, "Student " + student.getFullName() + " has not marked attendance");
                        }
                    } else {
                        student.setAttendanceStatus("Not Marked");
                        Log.d(TAG, "No attendance record found for " + student.getFullName());
                    }
                    studentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking attendance for " + student.getFullName(), e);
                    student.setAttendanceStatus("Not Marked");
                    studentAdapter.notifyDataSetChanged();
                });
    }

    private void setupStudentsListener() {
        if (classId == null || classId.isEmpty()) return;

        Log.d(TAG, "Setting up real-time attendance listener");

        studentsListener = db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to student attendance", error);
                        return;
                    }

                    if (snapshots != null) {
                        Log.d(TAG, "Attendance records updated: " + snapshots.size() + " records");

                        // Update each student's attendance status
                        for (var doc : snapshots.getDocuments()) {
                            String studentId = doc.getId();
                            Boolean marked = doc.getBoolean("marked");
                            Long timestamp = doc.getLong("timestamp");

                            Log.d(TAG, "Processing attendance update for studentId: " + studentId);

                            for (StudentAttendanceModel student : studentList) {
                                if (student.getStudentId() != null && student.getStudentId().equals(studentId)) {
                                    if (marked != null && marked && timestamp != null) {
                                        student.setMarked(true);
                                        student.setTimestamp(timestamp);
                                        String status = getAttendanceStatus(timestamp);
                                        student.setAttendanceStatus(status);
                                        Log.d(TAG, "Updated " + student.getFullName() + " status to: " + status);
                                    } else {
                                        student.setAttendanceStatus("Not Marked");
                                    }
                                    break;
                                }
                            }
                        }

                        // Also check for students who haven't marked yet
                        for (StudentAttendanceModel student : studentList) {
                            if (student.getStudentId() != null) {
                                boolean found = false;
                                for (var doc : snapshots.getDocuments()) {
                                    if (doc.getId().equals(student.getStudentId())) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    student.setAttendanceStatus("Not Marked");
                                    student.setMarked(false);
                                    student.setTimestamp(null);
                                }
                            }
                        }

                        studentAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Student adapter updated");
                    }
                });
    }

    private void updateStudentsHeaderVisibility() {
        boolean hasStudents = !studentList.isEmpty();
        Log.d(TAG, "Updating students header visibility. Has students: " + hasStudents + " Count: " + studentList.size());

        if (studentsAttendedHeader != null) {
            studentsAttendedHeader.setVisibility(hasStudents ? View.VISIBLE : View.GONE);
            Log.d(TAG, "Header visibility set to: " + (hasStudents ? "VISIBLE" : "GONE"));
        } else {
            Log.e(TAG, "studentsAttendedHeader is null!");
        }

        if (studentsRecyclerView != null) {
            studentsRecyclerView.setVisibility(hasStudents ? View.VISIBLE : View.GONE);
            Log.d(TAG, "RecyclerView visibility set to: " + (hasStudents ? "VISIBLE" : "GONE"));
        } else {
            Log.e(TAG, "studentsRecyclerView is null!");
        }
    }

    private void removeStudentFromClass(StudentAttendanceModel student, int position) {
        if (classId == null || classId.isEmpty()) return;

        String teacherId = mAuth.getCurrentUser().getUid();

        // Remove from teacher's class
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("allowedStudentEmails", FieldValue.arrayRemove(student.getEmail()))
                .addOnSuccessListener(unused -> {
                    // Remove from allClasses
                    db.collection("allClasses")
                            .document(classId)
                            .update("allowedStudentEmails", FieldValue.arrayRemove(student.getEmail()))
                            .addOnSuccessListener(unused2 -> {
                                // Decrement student count
                                decrementStudentCount(teacherId);

                                // Remove from adapter
                                studentAdapter.removeStudent(position);
                                studentList.remove(student);
                                updateStudentsHeaderVisibility();

                                // Remove student's attendance record if exists
                                if (student.getStudentId() != null) {
                                    db.collection("allClasses")
                                            .document(classId)
                                            .collection("attendanceRecords")
                                            .document(student.getStudentId())
                                            .delete();

                                    // Remove from student's enrolledClasses
                                    db.collection("users")
                                            .document(student.getStudentId())
                                            .collection("enrolledClasses")
                                            .document(classId)
                                            .delete();
                                }

                                Toast.makeText(this, "Student removed successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to remove from allClasses", e);
                                Toast.makeText(this, "Failed to remove student", Toast.LENGTH_SHORT).show();
                                studentAdapter.notifyItemChanged(position);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove student", e);
                    Toast.makeText(this, "Failed to remove student", Toast.LENGTH_SHORT).show();
                    studentAdapter.notifyItemChanged(position);
                });
    }

    private void decrementStudentCount(String teacherId) {
        // Decrement in teacher's class collection
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("students", FieldValue.increment(-1))
                .addOnSuccessListener(unused -> {
                    // Also decrement in allClasses collection
                    db.collection("allClasses")
                            .document(classId)
                            .update("students", FieldValue.increment(-1))
                            .addOnSuccessListener(unused2 -> {
                                Log.d(TAG, "Student count decremented successfully");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to decrement count in allClasses", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to decrement student count", e);
                });
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

            Log.d(TAG, "Attendance time difference: " + differenceMinutes + " minutes");

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
                    Log.d("ClassInformation", "Attendance marked successfully at timestamp: " + timestamp);
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

                    // Reset all student statuses to "Not Marked"
                    for (StudentAttendanceModel student : studentList) {
                        student.setAttendanceStatus("Not Marked");
                        student.setMarked(false);
                        student.setTimestamp(null);
                    }
                    studentAdapter.notifyDataSetChanged();
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
                // Hide student list for students
                if (studentsAttendedHeader != null) {
                    studentsAttendedHeader.setVisibility(View.GONE);
                }
                if (studentsRecyclerView != null) {
                    studentsRecyclerView.setVisibility(View.GONE);
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
        if (studentsListener != null) {
            studentsListener.remove();
        }
    }
}