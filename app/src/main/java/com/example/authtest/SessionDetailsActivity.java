package com.example.authtest;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionDetailsActivity extends AppCompatActivity {

    private static final String TAG = "SessionDetailsActivity";
    private String classId;
    private String sessionId;
    private long sessionStartTime;
    private long sessionEndTime;
    private String classStartTime;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView totalStudentsCount;
    private TextView presentCount;
    private TextView lateCount;
    private TextView absentCount;
    private View statsCard;
    private RecyclerView studentsRecyclerView;
    private StudentAttendanceAdapter studentAdapter;
    private List<StudentAttendanceModel> studentList = new ArrayList<>();
    private List<StudentAttendanceModel> filteredStudentList = new ArrayList<>();
    private EditText studentSearchBar;
    private ImageView clearSearchButton;
    private TextView studentsAttendedHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_details);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Intent intent = getIntent();
        classId = intent.getStringExtra("CLASS_ID");
        sessionId = intent.getStringExtra("SESSION_ID");
        sessionStartTime = intent.getLongExtra("SESSION_START_TIME", 0);
        sessionEndTime = intent.getLongExtra("SESSION_END_TIME", 0);
        classStartTime = intent.getStringExtra("CLASS_START_TIME");

        if (classId == null || sessionId == null) {
            Toast.makeText(this, "Error: Invalid session data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        ImageView hamburgerIcon = findViewById(R.id.hamburger_icon);
        if (hamburgerIcon != null) {
            hamburgerIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();

        statsCard = findViewById(R.id.attendanceStatsCard);
        if (statsCard != null) {
            totalStudentsCount = statsCard.findViewById(R.id.totalStudentsCount);
            presentCount = statsCard.findViewById(R.id.presentCount);
            lateCount = statsCard.findViewById(R.id.lateCount);
            absentCount = statsCard.findViewById(R.id.absentCount);
        }

        studentsRecyclerView = findViewById(R.id.studentsRecyclerView);
        studentSearchBar = findViewById(R.id.studentSearchBar);
        clearSearchButton = findViewById(R.id.clearSearchButton);
        studentsAttendedHeader = findViewById(R.id.studentsAttendedHeader);

        View backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        setupStudentListRecyclerView();
        setupStudentSearch();
        setupSwipeToDelete();

        loadSessionAttendanceData();
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_home) {
                navigateToUserHome();
                return true;
            } else if (itemId == R.id.menu_profile) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Profile feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_settings) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_logout) {
                logout();
                return true;
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return false;
        });
    }

    private void navigateToUserHome() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    drawerLayout.closeDrawer(GravityCompat.START);

                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");

                        if (userType != null) {
                            if ("Student".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(SessionDetailsActivity.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(SessionDetailsActivity.this, TeacherHome.class));
                            } else {
                                Toast.makeText(SessionDetailsActivity.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SessionDetailsActivity.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(SessionDetailsActivity.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(SessionDetailsActivity.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(SessionDetailsActivity.this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            drawerLayout.closeDrawer(GravityCompat.START);
                        } else {
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
    }

    private void loadUserInfoInDrawer() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView userNameTextView = headerView.findViewById(R.id.drawer_user_name);
            TextView userEmailTextView = headerView.findViewById(R.id.drawer_user_email);

            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");

                            String fullName = "";
                            if (firstName != null && !firstName.isEmpty()) {
                                fullName = firstName;
                            }
                            if (lastName != null && !lastName.isEmpty()) {
                                fullName += (fullName.isEmpty() ? "" : " ") + lastName;
                            }

                            userNameTextView.setText(fullName.isEmpty() ? "User" : fullName);
                        } else {
                            userNameTextView.setText("User");
                        }
                    })
                    .addOnFailureListener(e -> userNameTextView.setText("User"));

            userEmailTextView.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        }
    }

    private void setupStudentListRecyclerView() {
        studentAdapter = new StudentAttendanceAdapter(sessionId, this::removeStudentFromSession);
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentsRecyclerView.setAdapter(studentAdapter);
        studentsRecyclerView.setNestedScrollingEnabled(true);
    }

    private void setupStudentSearch() {
        studentSearchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchQuery = s.toString().trim().toLowerCase();
                filterStudents(searchQuery);

                if (searchQuery.isEmpty()) {
                    clearSearchButton.setVisibility(View.GONE);
                } else {
                    clearSearchButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        clearSearchButton.setOnClickListener(v -> {
            studentSearchBar.setText("");
            filteredStudentList.clear();
            studentAdapter.setStudents(new ArrayList<>(studentList));
        });
    }

    private void filterStudents(String searchQuery) {
        filteredStudentList.clear();

        if (searchQuery.isEmpty()) {
            studentAdapter.setStudents(new ArrayList<>(studentList));
            return;
        }

        for (StudentAttendanceModel student : studentList) {
            String fullName = student.getFullName().toLowerCase();
            String firstName = (student.getFirstName() != null ? student.getFirstName() : "").toLowerCase();
            String lastName = (student.getLastName() != null ? student.getLastName() : "").toLowerCase();

            if (fullName.contains(searchQuery) ||
                    firstName.contains(searchQuery) ||
                    lastName.contains(searchQuery)) {
                filteredStudentList.add(student);
            }
        }

        studentAdapter.setStudents(new ArrayList<>(filteredStudentList));
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.parseColor("#C92A2A"));
            private final Drawable deleteIcon = ContextCompat.getDrawable(SessionDetailsActivity.this, android.R.drawable.ic_menu_delete);

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                StudentAttendanceModel student = studentAdapter.getStudentAt(position);

                if (student != null) {
                    RemoveStudentConfirmationDialog confirmDialog = new RemoveStudentConfirmationDialog(
                            SessionDetailsActivity.this,
                            student.getFullName(),
                            () -> removeStudentFromSession(student, position),
                            () -> studentAdapter.notifyItemChanged(position)
                    );
                    confirmDialog.show();
                } else {
                    studentAdapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                int backgroundCornerOffset = 20;

                if (dX < 0) {
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

    private void loadSessionAttendanceData() {
        if (classId == null || classId.isEmpty() || sessionId == null || sessionId.isEmpty()) {
            Toast.makeText(this, "Error: Invalid session data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Loading attendance for Session: " + sessionId + ", Class: " + classId);

        db.collection("allClasses")
                .document(classId)
                .collection("recentSessions")
                .document(sessionId)
                .collection("attendanceRecords")
                .get()
                .addOnSuccessListener(attendanceSnapshot -> {
                    studentList.clear();
                    Log.d(TAG, "Found " + attendanceSnapshot.size() + " attendance records");

                    final int[] loadedCount = {0};
                    final int totalRecords = attendanceSnapshot.size();

                    if (totalRecords == 0) {
                        Log.d(TAG, "No students in this session");
                        onAllStudentsLoaded();
                        return;
                    }

                    for (DocumentSnapshot doc : attendanceSnapshot.getDocuments()) {
                        String studentId = doc.getId();
                        Long timestamp = doc.getLong("timestamp");
                        Boolean marked = doc.getBoolean("marked");

                        Log.d(TAG, "Processing record - studentId: " + studentId + ", marked: " + marked + ", timestamp: " + timestamp);

                        // ✅ FIXED: Check if marked is true (or null which defaults to true)
                        if ((marked == null || marked) && timestamp != null) {
                            db.collection("users")
                                    .document(studentId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            String firstName = userDoc.getString("firstName");
                                            String lastName = userDoc.getString("lastName");
                                            String schoolEmail = userDoc.getString("schoolEmail");

                                            StudentAttendanceModel student = new StudentAttendanceModel(
                                                    studentId,
                                                    schoolEmail != null ? schoolEmail : "unknown@email.com",
                                                    firstName != null ? firstName : "Unknown",
                                                    lastName != null ? lastName : "User"
                                            );

                                            String status = getAttendanceStatus(timestamp);
                                            student.setAttendanceStatus(status);
                                            student.setMarked(true);
                                            student.setTimestamp(timestamp);

                                            studentList.add(student);
                                            Log.d(TAG, "✓ Added student: " + student.getFullName() + " - Status: " + status);
                                        }

                                        loadedCount[0]++;
                                        checkIfAllLoaded(loadedCount[0], totalRecords);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error loading user " + studentId, e);
                                        loadedCount[0]++;
                                        checkIfAllLoaded(loadedCount[0], totalRecords);
                                    });
                        } else {
                            Log.d(TAG, "Skipped - marked is false or timestamp is null");
                            loadedCount[0]++;
                            checkIfAllLoaded(loadedCount[0], totalRecords);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading attendance records", e);
                    Toast.makeText(this, "Error: Failed to load attendance data - " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkIfAllLoaded(int loadedCount, int totalRecords) {
        Log.d(TAG, "Progress: " + loadedCount + "/" + totalRecords + " students loaded");

        if (loadedCount == totalRecords) {
            Log.d(TAG, "All students loaded! Total: " + studentList.size());
            onAllStudentsLoaded();
        }
    }

    private void onAllStudentsLoaded() {
        Log.d(TAG, "=== ALL STUDENTS LOADED ===");
        Log.d(TAG, "Total students: " + studentList.size());

        sortStudentsByLastName();

        for (StudentAttendanceModel s : studentList) {
            Log.d(TAG, "  - " + s.getFullName() + " (" + s.getEmail() + ") Status: " + s.getAttendanceStatus());
        }

        filteredStudentList.clear();
        filteredStudentList.addAll(studentList);
        studentSearchBar.setText("");

        // ✅ CRITICAL: Update adapter with students
        studentAdapter.setStudents(new ArrayList<>(studentList));
        Log.d(TAG, "Adapter updated with " + studentList.size() + " students");

        // ✅ CRITICAL: Calculate stats
        calculateAndUpdateStats();
        Log.d(TAG, "Stats calculated and updated");

        // ✅ CRITICAL: Update UI visibility
        updateUI();
        Log.d(TAG, "UI updated");

        Log.d(TAG, "=== LOAD COMPLETE ===");
    }

    private void sortStudentsByLastName() {
        Collections.sort(studentList, (student1, student2) -> {
            String lastName1 = student1.getLastName() != null ? student1.getLastName() : "";
            String lastName2 = student2.getLastName() != null ? student2.getLastName() : "";
            int lastNameComparison = lastName1.compareToIgnoreCase(lastName2);

            if (lastNameComparison == 0) {
                String firstName1 = student1.getFirstName() != null ? student1.getFirstName() : "";
                String firstName2 = student2.getFirstName() != null ? student2.getFirstName() : "";
                return firstName1.compareToIgnoreCase(firstName2);
            }

            return lastNameComparison;
        });
    }

    private void calculateAndUpdateStats() {
        Log.d(TAG, "=== CALCULATING STATS ===");

        if (totalStudentsCount == null || presentCount == null ||
                lateCount == null || absentCount == null) {
            Log.e(TAG, "❌ ERROR: Stats TextViews are NULL!");
            Log.e(TAG, "  totalStudentsCount: " + (totalStudentsCount != null ? "OK" : "NULL"));
            Log.e(TAG, "  presentCount: " + (presentCount != null ? "OK" : "NULL"));
            Log.e(TAG, "  lateCount: " + (lateCount != null ? "OK" : "NULL"));
            Log.e(TAG, "  absentCount: " + (absentCount != null ? "OK" : "NULL"));
            return;
        }

        int total = studentList.size();
        int present = 0, late = 0, absent = 0;

        Log.d(TAG, "Total students: " + total);

        for (StudentAttendanceModel student : studentList) {
            String status = student.getAttendanceStatus();
            Log.d(TAG, "  " + student.getFullName() + ": " + status);

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

        Log.d(TAG, "Breakdown: Present=" + present + ", Late=" + late + ", Absent=" + absent);

        totalStudentsCount.setText(String.valueOf(total));
        presentCount.setText(String.valueOf(present));
        lateCount.setText(String.valueOf(late));
        absentCount.setText(String.valueOf(absent));

        Log.d(TAG, "✓ Stats updated on UI");
    }

    private String getAttendanceStatus(long markedTimestamp) {
        if (classStartTime == null || classStartTime.isEmpty()) {
            return "Present";
        }

        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date markedTime = new Date(markedTimestamp);

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

        } catch (Exception e) {
            return "Present";
        }
    }

    private void removeStudentFromSession(StudentAttendanceModel student, int position) {
        if (classId == null || sessionId == null) {
            Toast.makeText(this, "Error: Invalid session", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("allClasses")
                .document(classId)
                .collection("recentSessions")
                .document(sessionId)
                .collection("attendanceRecords")
                .document(student.getStudentId())
                .delete()
                .addOnSuccessListener(unused -> {
                    studentAdapter.removeStudent(position);
                    studentList.remove(student);
                    calculateAndUpdateStats();
                    updateUI();
                    Toast.makeText(this, "Student removed from session", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove student", Toast.LENGTH_SHORT).show();
                    studentAdapter.notifyItemChanged(position);
                });
    }

    private void updateUI() {
        Log.d(TAG, "Updating UI visibility");

        // ✅ Show stats card
        if (statsCard != null) {
            statsCard.setVisibility(View.VISIBLE);
            Log.d(TAG, "✓ Stats card visibility: VISIBLE");
        } else {
            Log.e(TAG, "✗ statsCard is NULL!");
        }

        // ✅ Show students header
        if (studentsAttendedHeader != null) {
            studentsAttendedHeader.setVisibility(View.VISIBLE);
            Log.d(TAG, "✓ Students header visibility: VISIBLE");
        } else {
            Log.e(TAG, "✗ studentsAttendedHeader is NULL!");
        }

        // ✅ Show search bar
        View searchBarContainer = findViewById(R.id.searchBarContainer);
        if (searchBarContainer != null) {
            searchBarContainer.setVisibility(View.VISIBLE);
            Log.d(TAG, "✓ Search bar visibility: VISIBLE");
        } else {
            Log.e(TAG, "✗ searchBarContainer is NULL!");
        }

        // ✅ Show/hide RecyclerView based on student list
        boolean hasStudents = !studentList.isEmpty();
        if (studentsRecyclerView != null) {
            studentsRecyclerView.setVisibility(hasStudents ? View.VISIBLE : View.GONE);
            Log.d(TAG, "✓ RecyclerView visibility: " + (hasStudents ? "VISIBLE" : "GONE"));
        } else {
            Log.e(TAG, "✗ studentsRecyclerView is NULL!");
        }

        Log.d(TAG, "UI update complete");
    }

    private void debugAttendanceRecords() {
        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .get()
                .addOnSuccessListener(classSnapshot -> {
                    db.collection("allClasses")
                            .document(classId)
                            .collection("recentSessions")
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(sessionSnapshot -> {
                                if (sessionSnapshot.isEmpty()) {
                                    return;
                                }

                                String sessionId = sessionSnapshot.getDocuments().get(0).getString("sessionId");

                                db.collection("allClasses")
                                        .document(classId)
                                        .collection("recentSessions")
                                        .document(sessionId)
                                        .collection("attendanceRecords")
                                        .get();
                            });
                });
    }
}