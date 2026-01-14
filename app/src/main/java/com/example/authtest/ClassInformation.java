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
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.authtest.databinding.ActivityClassInformationBinding;
import com.example.authtest.databinding.AttendanceCardBinding;
import com.example.authtest.databinding.StudentsAttendanceStatusCardBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

 import androidx.core.content.ContextCompat;
 import android.graphics.drawable.Drawable;
 import android.graphics.Canvas;
 import android.graphics.drawable.ColorDrawable;
 import androidx.annotation.NonNull;

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
    private RecyclerView studentsRecyclerView;
    private StudentAttendanceAdapter studentAdapter;
    private List<StudentAttendanceModel> studentList = new ArrayList<>();
    private TextView studentsAttendedHeader;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private EditText studentSearchBar;
    private ImageView clearSearchButton;
    private List<StudentAttendanceModel> filteredStudentList = new ArrayList<>();
    private RecyclerView recentSessionsRecyclerView;
    private RecentSessionAdapter recentSessionAdapter;
    private List<RecentSession> recentSessionList = new ArrayList<>();
    private ListenerRegistration recentSessionsListener;
    private long attendanceSessionStartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityClassInformationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize DrawerLayout and NavigationView using findViewById
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        // Hamburger icon click listener
        binding.hamburgerIcon.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        studentSearchBar = findViewById(R.id.studentSearchBar);
        clearSearchButton = findViewById(R.id.clearSearchButton);

        setupStudentSearch();

        // Setup navigation drawer menu items
        setupNavigationDrawer();

        // Load user info in drawer header
        loadUserInfoInDrawer();

        // Setup back press handler
        setupBackPressHandler();

        binding.backButton.setOnClickListener(v -> finish());

        addStudentsButton = findViewById(R.id.addStudentsButton);

        View statsCard = binding.attendanceStatsCard.getRoot();
        totalStudentsCount = statsCard.findViewById(R.id.totalStudentsCount);
        presentCount = statsCard.findViewById(R.id.presentCount);
        lateCount = statsCard.findViewById(R.id.lateCount);
        absentCount = statsCard.findViewById(R.id.absentCount);

        studentsRecyclerView = findViewById(R.id.studentsRecyclerView);
        studentsAttendedHeader = findViewById(R.id.studentsAttendedHeader);
        recentSessionsRecyclerView = findViewById(R.id.recentSessionsRecyclerView);

        // Initialize recent sessions for BOTH teachers and students
        setupRecentSessionsRecyclerView();

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
                    diagnoseFirestoreStructure();
                    loadStudentList();
                } else {
                    setupStudentView(classModel);
                }

                loadRecentSessions();
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

    private void setupRecentSessionsRecyclerView() {
        if (recentSessionsRecyclerView == null) {
            recentSessionsRecyclerView = findViewById(R.id.recentSessionsRecyclerView);
        }

        Log.d(TAG, "Setting up recent sessions RecyclerView");
        recentSessionAdapter = new RecentSessionAdapter();
        recentSessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentSessionsRecyclerView.setAdapter(recentSessionAdapter);
        recentSessionsRecyclerView.setNestedScrollingEnabled(false);

        recentSessionAdapter.setClickListener(session -> {
            navigateToSessionDetails(session);
        });
        recentSessionAdapter.setRemoveListener((session, position) -> {
            removeSessionFromClass(session, position);
        });

        setupSwipeToDeleteSessions();

        Log.d(TAG, "✓ Recent sessions adapter initialized");
    }

    private void setupSwipeToDeleteSessions() {
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
                RecentSession session = recentSessionAdapter.getSessionAt(position);

                if (session != null) {
                    RemoveSessionConfirmationDialog confirmDialog = new RemoveSessionConfirmationDialog(
                            ClassInformation.this,
                            session.getDate(),
                            () -> {
                                recentSessionAdapter.triggerRemoval(session, position);
                            },
                            () -> {
                                recentSessionAdapter.notifyItemChanged(position);
                            }
                    );
                    confirmDialog.show();
                } else {
                    recentSessionAdapter.notifyItemChanged(position);
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
        itemTouchHelper.attachToRecyclerView(recentSessionsRecyclerView);
    }

    private void navigateToSessionDetails(RecentSession session) {
        Intent intent = new Intent(ClassInformation.this, SessionDetailsActivity.class);
        intent.putExtra("CLASS_ID", classId);
        intent.putExtra("SESSION_ID", session.getSessionId());
        intent.putExtra("SESSION_START_TIME", session.getSessionStartTime());
        intent.putExtra("SESSION_END_TIME", session.getSessionEndTime());
        intent.putExtra("CLASS_START_TIME", classStartTime);
        startActivity(intent);
    }

    private void removeSessionFromClass(RecentSession session, int position) {
        if (classId == null || session.getSessionId() == null) {
            Log.e(TAG, "Cannot delete session - classId or sessionId is null");
            return;
        }

        String sessionId = session.getSessionId();
        String teacherId = mAuth.getCurrentUser().getUid();

        Log.d(TAG, "Starting removal of session: " + sessionId);

        // Delete from allClasses/classId/recentSessions
        db.collection("allClasses")
                .document(classId)
                .collection("recentSessions")
                .document(sessionId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✓ Removed from allClasses recentSessions");

                    // Delete from users/teacherId/classes/classId/recentSessions
                    db.collection("users")
                            .document(teacherId)
                            .collection("classes")
                            .document(classId)
                            .collection("recentSessions")
                            .document(sessionId)
                            .delete()
                            .addOnSuccessListener(unused2 -> {
                                Log.d(TAG, "✓ Removed from teacher's recentSessions");

                                try {
                                    if (position >= 0 && position < recentSessionList.size()) {
                                        recentSessionList.remove(position);
                                        recentSessionAdapter.notifyItemRemoved(position);
                                        recentSessionAdapter.notifyItemRangeChanged(position, recentSessionList.size());
                                        Log.d(TAG, "✓ Updated adapter at position " + position);
                                    } else {
                                        Log.w(TAG, "Position no longer valid, reloading sessions");
                                        loadRecentSessions();
                                        return;
                                    }

                                    if (recentSessionList.isEmpty()) {
                                        updateRecentSessionsVisibility();
                                    }

                                    Toast.makeText(ClassInformation.this, "Session deleted successfully", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "✓ Session deleted successfully");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error updating adapter", e);
                                    loadRecentSessions();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to remove from teacher's recentSessions", e);
                                Toast.makeText(ClassInformation.this, "Failed to delete session", Toast.LENGTH_SHORT).show();
                                try {
                                    if (position >= 0 && position < recentSessionList.size()) {
                                        recentSessionAdapter.notifyItemChanged(position);
                                    }
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error notifying adapter", ex);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove session", e);
                    Toast.makeText(ClassInformation.this, "Failed to delete session", Toast.LENGTH_SHORT).show();
                    try {
                        if (position >= 0 && position < recentSessionList.size()) {
                            recentSessionAdapter.notifyItemChanged(position);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error notifying adapter", ex);
                    }
                });
    }

    private void saveRecentSession(long endTime) {
        if (classId == null || classId.isEmpty()) {
            Log.e(TAG, "❌ CRITICAL ERROR: Cannot save session - classId is null");
            return;
        }

        long startTime = attendanceSessionStartTime > 0 ? attendanceSessionStartTime : (endTime - 3600000);
        String sessionId = String.valueOf(System.currentTimeMillis());

        Log.d(TAG, "");
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║    SAVING SESSION TO FIRESTORE         ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
        Log.d(TAG, "sessionId: " + sessionId);
        Log.d(TAG, "classId: " + classId);
        Log.d(TAG, "startTime: " + startTime);
        Log.d(TAG, "endTime: " + endTime);
        Log.d(TAG, "Duration: " + ((endTime - startTime) / 1000 / 60) + " minutes");
        Log.d(TAG, "");

        // Create session document
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", sessionId);
        sessionData.put("classId", classId);
        sessionData.put("sessionStartTime", startTime);
        sessionData.put("sessionEndTime", endTime);
        sessionData.put("timestamp", endTime);

        Log.d(TAG, "Creating session document at: allClasses/" + classId + "/recentSessions/" + sessionId);

        db.collection("allClasses")
                .document(classId)
                .collection("recentSessions")
                .document(sessionId)
                .set(sessionData)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✅ SUCCESS: Session document created!");
                    Log.d(TAG, "→ Now copying attendance records...");

                    // Step 2: Copy attendance records to the session
                    saveAttendanceRecordsToSession(sessionId, startTime, endTime);

                    // Step 3: Also save to teacher's classes
                    String teacherId = mAuth.getCurrentUser().getUid();
                    db.collection("users")
                            .document(teacherId)
                            .collection("classes")
                            .document(classId)
                            .collection("recentSessions")
                            .document(sessionId)
                            .set(sessionData)
                            .addOnSuccessListener(unused2 -> {
                                Log.d(TAG, "✅ Session also saved to teacher's personal classes");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "⚠ Warning: Failed to save session to teacher's classes: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ CRITICAL ERROR: Failed to create session document");
                    Log.e(TAG, "Error message: " + e.getMessage());
                    Log.e(TAG, "Error code: " + (e.getCause() != null ? e.getCause().toString() : "Unknown"));
                    Toast.makeText(ClassInformation.this, "Failed to save session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveAttendanceRecordsToSession(String sessionId, long startTime, long endTime) {
        Log.d(TAG, "Step 2: Copying attendance records to session...");

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int recordCount = querySnapshot.size();
                    Log.d(TAG, "Found " + recordCount + " attendance records at class level");

                    if (recordCount == 0) {
                        Log.d(TAG, "⚠ No students marked attendance (empty session)");
                        clearAllAttendanceRecords();
                        return;
                    }

                    final int[] savedCount = {0};
                    final int[] skippedCount = {0};
                    final int[] failedCount = {0};

                    for (var doc : querySnapshot.getDocuments()) {
                        String studentId = doc.getId();
                        Long timestamp = doc.getLong("timestamp");
                        Boolean marked = doc.getBoolean("marked");

                        Log.d(TAG, "Processing record - studentId: " + studentId + ", marked: " + marked + ", timestamp: " + timestamp);

                        // FIX: Copy ALL records that have a timestamp, regardless of marked status
                        if (timestamp != null) {
                            // If marked is null, default to true (student who marked attendance)
                            boolean isMarked = marked != null ? marked : true;

                            Map<String, Object> recordData = new HashMap<>();
                            recordData.put("studentId", studentId);
                            recordData.put("timestamp", timestamp);
                            recordData.put("marked", isMarked);

                            Log.d(TAG, "  → Copying record for " + studentId + " (marked=" + isMarked + ")");

                            db.collection("allClasses")
                                    .document(classId)
                                    .collection("recentSessions")
                                    .document(sessionId)
                                    .collection("attendanceRecords")
                                    .document(studentId)
                                    .set(recordData)
                                    .addOnSuccessListener(unused -> {
                                        savedCount[0]++;
                                        Log.d(TAG, "  ✅ Copied record " + savedCount[0] + "/" + recordCount);

                                        checkIfAllRecordsCopied(savedCount[0], skippedCount[0], failedCount[0], recordCount);
                                    })
                                    .addOnFailureListener(e -> {
                                        failedCount[0]++;
                                        Log.e(TAG, "  ❌ Failed to copy record for " + studentId + ": " + e.getMessage());

                                        checkIfAllRecordsCopied(savedCount[0], skippedCount[0], failedCount[0], recordCount);
                                    });
                        } else {
                            skippedCount[0]++;
                            Log.d(TAG, "  ⊘ Skipped record for " + studentId + " (no timestamp)");
                            checkIfAllRecordsCopied(savedCount[0], skippedCount[0], failedCount[0], recordCount);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ CRITICAL ERROR: Failed to query attendance records: " + e.getMessage());
                    Toast.makeText(ClassInformation.this, "Failed to save session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkIfAllRecordsCopied(int savedCount, int skippedCount, int failedCount, int totalCount) {
        int processedCount = savedCount + skippedCount + failedCount;

        Log.d(TAG, "Progress: " + processedCount + "/" + totalCount + " (Saved: " + savedCount + ", Skipped: " + skippedCount + ", Failed: " + failedCount + ")");

        if (processedCount >= totalCount) {
            Log.d(TAG, "");
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║  ATTENDANCE RECORDS COPY COMPLETE      ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");
            Log.d(TAG, "Saved: " + savedCount);
            Log.d(TAG, "Skipped: " + skippedCount);
            Log.d(TAG, "Failed: " + failedCount);
            Log.d(TAG, "Total: " + totalCount);
            Log.d(TAG, "");

            // Now clear the class-level records
            clearAllAttendanceRecords();
        }
    }

    private void onAttendanceRecordsCopied(int savedCount, int failedCount) {
        int totalProcessed = savedCount + failedCount;
        Log.d(TAG, "✓ Step 2 Complete: Copied " + savedCount + " records (Failed: " + failedCount + ")");

        if (savedCount > 0) {
            Log.d(TAG, "✓✓✓ Attendance records successfully copied to session");
        } else {
            Log.d(TAG, "⚠ No marked attendance records were copied (session is empty)");
        }
    }

    private void loadRecentSessions() {
        if (classId == null || classId.isEmpty()) {
            Log.e(TAG, "Cannot load sessions - classId is null");
            return;
        }

        Log.d(TAG, "🔄 Loading recent sessions for class: " + classId);

        db.collection("allClasses")
                .document(classId)
                .collection("recentSessions")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    recentSessionList.clear();

                    Log.d(TAG, "📦 QuerySnapshot returned: " + querySnapshot.size() + " sessions");

                    for (var doc : querySnapshot.getDocuments()) {
                        long startTime = doc.getLong("sessionStartTime") != null ? doc.getLong("sessionStartTime") : 0;
                        long endTime = doc.getLong("sessionEndTime") != null ? doc.getLong("sessionEndTime") : 0;
                        String sessionId = doc.getString("sessionId");

                        RecentSession session = new RecentSession(sessionId, classId, startTime, endTime);
                        recentSessionList.add(session);

                        Log.d(TAG, "  ✓ Loaded session: " + session.getDate() + " " + session.getSessionTimeRange());
                    }

                    Log.d(TAG, "📊 Total sessions loaded: " + recentSessionList.size());

                    if (recentSessionAdapter != null) {
                        recentSessionAdapter.setSessions(new ArrayList<>(recentSessionList));
                        Log.d(TAG, "✓ Adapter updated with sessions");
                    } else {
                        Log.e(TAG, "✗ recentSessionAdapter is NULL - cannot update!");
                    }

                    updateRecentSessionsVisibility();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading recent sessions", e);
                });
    }

    private void setupRecentSessionsListener() {
        if (classId == null || classId.isEmpty()) {
            Log.e(TAG, "Cannot setup listener - classId is null");
            return;
        }

        Log.d(TAG, "🔌 Setting up real-time recent sessions listener for class: " + classId);

        if (recentSessionsListener != null) {
            recentSessionsListener.remove();
        }

        recentSessionsListener = db.collection("allClasses")
                .document(classId)
                .collection("recentSessions")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to recent sessions", error);
                        return;
                    }

                    if (snapshots != null) {
                        Log.d(TAG, "🔔 Recent sessions updated! Total: " + snapshots.size());

                        recentSessionList.clear();

                        for (var doc : snapshots.getDocuments()) {
                            long startTime = doc.getLong("sessionStartTime") != null ? doc.getLong("sessionStartTime") : 0;
                            long endTime = doc.getLong("sessionEndTime") != null ? doc.getLong("sessionEndTime") : 0;
                            String sessionId = doc.getString("sessionId");

                            RecentSession session = new RecentSession(sessionId, classId, startTime, endTime);
                            recentSessionList.add(session);

                            Log.d(TAG, "  ✓ Session: " + session.getDate() + " " + session.getSessionTimeRange());
                        }

                        if (recentSessionAdapter != null) {
                            recentSessionAdapter.setSessions(new ArrayList<>(recentSessionList));
                            Log.d(TAG, "✓ Adapter updated with " + recentSessionList.size() + " sessions");
                        } else {
                            Log.e(TAG, "✗ recentSessionAdapter is NULL!");
                        }

                        updateRecentSessionsVisibility();
                    }
                });

        Log.d(TAG, "✓ Listener attached successfully");
    }

    private void updateRecentSessionsVisibility() {
        boolean hasSessions = !recentSessionList.isEmpty();
        Log.d(TAG, "Updating recent sessions visibility. Has sessions: " + hasSessions + " Count: " + recentSessionList.size());

        View recentSessionsHeader = findViewById(R.id.recentSessionsHeader);
        View recentSessionsRecycler = findViewById(R.id.recentSessionsRecyclerView);

        if (recentSessionsHeader != null) {
            recentSessionsHeader.setVisibility(hasSessions ? View.VISIBLE : View.GONE);
            Log.d(TAG, "✓ Header visibility set to: " + (hasSessions ? "VISIBLE" : "GONE"));
        } else {
            Log.e(TAG, "✗ recentSessionsHeader is NULL");
        }

        if (recentSessionsRecycler != null) {
            recentSessionsRecycler.setVisibility(hasSessions ? View.VISIBLE : View.GONE);
            Log.d(TAG, "✓ RecyclerView visibility set to: " + (hasSessions ? "VISIBLE" : "GONE"));
        } else {
            Log.e(TAG, "✗ recentSessionsRecyclerView is NULL");
        }
    }

    private void setupStudentSearch() {
        studentSearchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchQuery = s.toString().trim().toLowerCase();
                filterStudents(searchQuery);

                // Show/hide clear button
                if (searchQuery.isEmpty()) {
                    clearSearchButton.setVisibility(View.GONE);
                } else {
                    clearSearchButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Clear button click listener
        clearSearchButton.setOnClickListener(v -> {
            studentSearchBar.setText("");
            filteredStudentList.clear();
            studentAdapter.setStudents(new ArrayList<>(studentList));
        });
    }

    private void filterStudents(String searchQuery) {
        filteredStudentList.clear();

        if (searchQuery.isEmpty()) {
            // Show all students if search is empty
            studentAdapter.setStudents(new ArrayList<>(studentList));
            return;
        }

        // Filter students by first name or last name
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

        // Update adapter with filtered results
        studentAdapter.setStudents(new ArrayList<>(filteredStudentList));

        Log.d(TAG, "Search for '" + searchQuery + "' returned " + filteredStudentList.size() + " results");
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
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
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Streak feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Archive feature coming soon", Toast.LENGTH_SHORT).show();
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
                                startActivity(new Intent(ClassInformation.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(ClassInformation.this, TeacherHome.class));
                            } else {
                                Toast.makeText(ClassInformation.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ClassInformation.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ClassInformation.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(ClassInformation.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(ClassInformation.this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadUserInfoInDrawer() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView userNameTextView = headerView.findViewById(R.id.drawer_user_name);
            TextView userEmailTextView = headerView.findViewById(R.id.drawer_user_email);

            // Fetch user details from Firestore
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
                    .addOnFailureListener(e -> {
                        userNameTextView.setText("User");
                    });

            userEmailTextView.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        }
    }

    private void diagnoseFirestoreStructure() {
        db.collection("allClasses")
                .document(classId)
                .get()
                .addOnSuccessListener(classDoc -> {
                    if (classDoc.exists()) {
                        List<String> allowedEmails = (List<String>) classDoc.get("allowedStudentEmails");
                        Log.d(TAG, "DIAGNOSTIC: allowedStudentEmails = " + allowedEmails);

                        if (allowedEmails != null && !allowedEmails.isEmpty()) {
                            String targetEmail = allowedEmails.get(0);
                            Log.d(TAG, "DIAGNOSTIC: Target email = '" + targetEmail + "'");

                            db.collection("users")
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        Log.d(TAG, "DIAGNOSTIC: Total users in database = " + querySnapshot.size());

                                        boolean foundMatch = false;

                                        for (var doc : querySnapshot.getDocuments()) {
                                            Map<String, Object> allFields = doc.getData();

                                            Log.d(TAG, "----------------------------------------");
                                            Log.d(TAG, "DIAGNOSTIC: User ID = " + doc.getId());
                                            Log.d(TAG, "DIAGNOSTIC: All fields = " + allFields.keySet());

                                            String schoolEmail = doc.getString("schoolEmail");
                                            String email = doc.getString("email");
                                            String userEmail = doc.getString("userEmail");
                                            String studentEmail = doc.getString("studentEmail");
                                            String emailAddress = doc.getString("emailAddress");

                                            Log.d(TAG, "DIAGNOSTIC:   schoolEmail = " + schoolEmail);
                                            Log.d(TAG, "DIAGNOSTIC:   email = " + email);
                                            Log.d(TAG, "DIAGNOSTIC:   userEmail = " + userEmail);
                                            Log.d(TAG, "DIAGNOSTIC:   studentEmail = " + studentEmail);
                                            Log.d(TAG, "DIAGNOSTIC:   emailAddress = " + emailAddress);

                                            String firstName = doc.getString("firstName");
                                            String lastName = doc.getString("lastName");
                                            String first_name = doc.getString("first_name");
                                            String last_name = doc.getString("last_name");

                                            Log.d(TAG, "DIAGNOSTIC:   firstName = " + firstName);
                                            Log.d(TAG, "DIAGNOSTIC:   lastName = " + lastName);
                                            Log.d(TAG, "DIAGNOSTIC:   first_name = " + first_name);
                                            Log.d(TAG, "DIAGNOSTIC:   last_name = " + last_name);

                                            String cleanTarget = targetEmail.toLowerCase().trim();

                                            if ((schoolEmail != null && schoolEmail.toLowerCase().trim().equals(cleanTarget)) ||
                                                    (email != null && email.toLowerCase().trim().equals(cleanTarget)) ||
                                                    (userEmail != null && userEmail.toLowerCase().trim().equals(cleanTarget)) ||
                                                    (studentEmail != null && studentEmail.toLowerCase().trim().equals(cleanTarget)) ||
                                                    (emailAddress != null && emailAddress.toLowerCase().trim().equals(cleanTarget))) {

                                                foundMatch = true;
                                                Log.d(TAG, "==========================================");
                                                Log.d(TAG, "✓✓✓ FOUND MATCHING USER! ✓✓✓");
                                                Log.d(TAG, "==========================================");
                                                Log.d(TAG, "DIAGNOSTIC: User ID = " + doc.getId());
                                                Log.d(TAG, "DIAGNOSTIC: First Name = " + (firstName != null ? firstName : first_name));
                                                Log.d(TAG, "DIAGNOSTIC: Last Name = " + (lastName != null ? lastName : last_name));
                                                Log.d(TAG, "DIAGNOSTIC: Matching email field value = " +
                                                        (schoolEmail != null && schoolEmail.toLowerCase().trim().equals(cleanTarget) ? "schoolEmail: " + schoolEmail :
                                                                email != null && email.toLowerCase().trim().equals(cleanTarget) ? "email: " + email :
                                                                        userEmail != null && userEmail.toLowerCase().trim().equals(cleanTarget) ? "userEmail: " + userEmail :
                                                                                studentEmail != null && studentEmail.toLowerCase().trim().equals(cleanTarget) ? "studentEmail: " + studentEmail :
                                                                                        "emailAddress: " + emailAddress));
                                                Log.d(TAG, "==========================================");
                                            }
                                        }

                                        if (!foundMatch) {
                                            Log.e(TAG, "==========================================");
                                            Log.e(TAG, "✗✗✗ NO MATCHING USER FOUND! ✗✗✗");
                                            Log.e(TAG, "==========================================");
                                            Log.e(TAG, "DIAGNOSTIC: Looking for email = '" + targetEmail + "'");
                                            Log.e(TAG, "DIAGNOSTIC: This email does NOT exist in any user document!");
                                            Log.e(TAG, "DIAGNOSTIC: Check if:");
                                            Log.e(TAG, "  1. The student has registered with this email");
                                            Log.e(TAG, "  2. The email has correct spelling");
                                            Log.e(TAG, "  3. The email case matches (should be lowercase)");
                                            Log.e(TAG, "==========================================");
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "DIAGNOSTIC: Error loading users", e);
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "DIAGNOSTIC: Error loading class", e);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            Log.d(TAG, "Returning from AddStudentsForm, refreshing list");
            loadStudentList();
        }
    }

    private void setupStudentListRecyclerView() {
        Log.d(TAG, "Setting up student list RecyclerView");
        studentAdapter = new StudentAttendanceAdapter(classId, this::removeStudentFromClass);
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

                if (student != null) {
                    RemoveStudentConfirmationDialog confirmDialog = new RemoveStudentConfirmationDialog(
                            ClassInformation.this,
                            student.getFullName(),
                            () -> {
                                removeStudentFromClass(student, position);
                            },
                            () -> {
                                studentAdapter.notifyItemChanged(position);
                            }
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

                            db.collection("users")
                                    .whereEqualTo("schoolEmail", cleanEmail)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        if (!querySnapshot.isEmpty()) {
                                            var userDoc = querySnapshot.getDocuments().get(0);
                                            String firstName = userDoc.getString("firstName");
                                            String lastName = userDoc.getString("lastName");
                                            String studentId = userDoc.getId();
                                            String userEmail = userDoc.getString("schoolEmail");

                                            Log.d(TAG, "✓ Found student: " + firstName + " " + lastName + " (ID: " + studentId + ")");

                                            StudentAttendanceModel student = new StudentAttendanceModel(
                                                    studentId,
                                                    userEmail != null ? userEmail : email,
                                                    firstName != null ? firstName : "Unknown",
                                                    lastName != null ? lastName : "User"
                                            );

                                            studentList.add(student);
                                            checkStudentAttendance(student);
                                        } else {
                                            Log.w(TAG, "No user found with schoolEmail field, trying 'email' field");

                                            db.collection("users")
                                                    .whereEqualTo("email", cleanEmail)
                                                    .limit(1)
                                                    .get()
                                                    .addOnSuccessListener(altQuerySnapshot -> {
                                                        if (!altQuerySnapshot.isEmpty()) {
                                                            var userDoc = altQuerySnapshot.getDocuments().get(0);
                                                            String firstName = userDoc.getString("firstName");
                                                            String lastName = userDoc.getString("lastName");
                                                            String studentId = userDoc.getId();
                                                            String userEmail = userDoc.getString("email");

                                                            Log.d(TAG, "✓ Found student via 'email' field: " + firstName + " " + lastName);

                                                            StudentAttendanceModel student = new StudentAttendanceModel(
                                                                    studentId,
                                                                    userEmail != null ? userEmail : email,
                                                                    firstName != null ? firstName : "Unknown",
                                                                    lastName != null ? lastName : "User"
                                                            );

                                                            studentList.add(student);
                                                            checkStudentAttendance(student);
                                                        } else {
                                                            Log.e(TAG, "✗ User not found in Firestore for: " + cleanEmail);
                                                            StudentAttendanceModel student = new StudentAttendanceModel(
                                                                    null, email, "Unknown", "User"
                                                            );
                                                            student.setAttendanceStatus("Not Marked");
                                                            studentList.add(student);
                                                        }

                                                        loadedCount[0]++;
                                                        checkIfAllLoaded(loadedCount[0], totalEmails);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "Error in fallback query", e);
                                                        StudentAttendanceModel student = new StudentAttendanceModel(
                                                                null, email, "Unknown", "User"
                                                        );
                                                        student.setAttendanceStatus("Not Marked");
                                                        studentList.add(student);

                                                        loadedCount[0]++;
                                                        checkIfAllLoaded(loadedCount[0], totalEmails);
                                                    });
                                        }

                                        loadedCount[0]++;
                                        checkIfAllLoaded(loadedCount[0], totalEmails);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error querying user: " + cleanEmail, e);
                                        StudentAttendanceModel student = new StudentAttendanceModel(
                                                null, email, "Unknown", "User"
                                        );
                                        student.setAttendanceStatus("Not Marked");
                                        studentList.add(student);

                                        loadedCount[0]++;
                                        checkIfAllLoaded(loadedCount[0], totalEmails);
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

    private void checkIfAllLoaded(int loadedCount, int totalEmails) {
        if (loadedCount == totalEmails) {
            Log.d(TAG, "=== All " + totalEmails + " students loaded ===");
            Log.d(TAG, "Students in list: " + studentList.size());

            for (StudentAttendanceModel s : studentList) {
                Log.d(TAG, "  - " + s.getFullName() + " (" + s.getEmail() + ") Status: " + s.getAttendanceStatus());
            }

            sortStudentsByLastName();

            // Initialize filtered list with all students
            filteredStudentList.clear();
            filteredStudentList.addAll(studentList);

            // Clear search bar
            studentSearchBar.setText("");

            studentAdapter.setStudents(new ArrayList<>(studentList));
            updateStudentsHeaderVisibility();
            setupStudentsListener();
        }
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

        Log.d(TAG, "Students sorted alphabetically by last name");
        for (StudentAttendanceModel s : studentList) {
            Log.d(TAG, "  - " + s.getFullName());
        }
    }

    private void checkStudentAttendance(StudentAttendanceModel student) {
        if (classId == null || student.getStudentId() == null) {
            Log.d(TAG, "Cannot check attendance - classId or studentId is null for: " + student.getEmail());
            student.setAttendanceStatus("Not Marked");
            student.setMarked(false);
            student.setTimestamp(null);
            return;
        }

        Log.d(TAG, "Checking attendance for: " + student.getFullName() + " (ID: " + student.getStudentId() + ")");

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .document(student.getStudentId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean marked = doc.getBoolean("marked");
                        Long timestamp = doc.getLong("timestamp");

                        Log.d(TAG, "✓ Attendance record EXISTS for " + student.getFullName() + ": marked=" + marked + ", timestamp=" + timestamp);

                        if (marked != null && marked && timestamp != null) {
                            student.setMarked(true);
                            student.setTimestamp(timestamp);
                            String status = getAttendanceStatus(timestamp);
                            student.setAttendanceStatus(status);
                            Log.d(TAG, "  → Status set to: " + status);
                        } else {
                            student.setAttendanceStatus("Not Marked");
                            student.setMarked(false);
                            student.setTimestamp(null);
                            Log.d(TAG, "  → Marked is false or timestamp is null");
                        }
                    } else {
                        student.setAttendanceStatus("Not Marked");
                        student.setMarked(false);
                        student.setTimestamp(null);
                        Log.d(TAG, "✗ No attendance record for " + student.getFullName());
                    }

                    studentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking attendance for " + student.getFullName(), e);
                    student.setAttendanceStatus("Not Marked");
                    student.setMarked(false);
                    student.setTimestamp(null);
                    studentAdapter.notifyDataSetChanged();
                });
    }

    private void setupStudentsListener() {
        if (classId == null || classId.isEmpty()) {
            Log.e(TAG, "Cannot setup listener - classId is null");
            return;
        }

        Log.d(TAG, "=== Setting up real-time attendance listener ===");

        if (studentsListener != null) {
            studentsListener.remove();
        }

        studentsListener = db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to student attendance", error);
                        return;
                    }

                    if (snapshots != null) {
                        Log.d(TAG, "🔔 Attendance records updated! Total records: " + snapshots.size());

                        java.util.Map<String, com.google.firebase.firestore.DocumentSnapshot> attendanceMap = new java.util.HashMap<>();
                        for (var doc : snapshots.getDocuments()) {
                            attendanceMap.put(doc.getId(), doc);
                            Log.d(TAG, "  Record: " + doc.getId() + " marked=" + doc.getBoolean("marked"));
                        }

                        boolean updated = false;
                        for (StudentAttendanceModel student : studentList) {
                            if (student.getStudentId() != null) {
                                var attendanceDoc = attendanceMap.get(student.getStudentId());

                                if (attendanceDoc != null) {
                                    Boolean marked = attendanceDoc.getBoolean("marked");
                                    Long timestamp = attendanceDoc.getLong("timestamp");

                                    if (marked != null && marked && timestamp != null) {
                                        String newStatus = getAttendanceStatus(timestamp);
                                        String oldStatus = student.getAttendanceStatus();

                                        if (!newStatus.equals(oldStatus)) {
                                            Log.d(TAG, "  ✓ Updating " + student.getFullName() + ": " + oldStatus + " → " + newStatus);
                                            updated = true;
                                        }

                                        student.setMarked(true);
                                        student.setTimestamp(timestamp);
                                        student.setAttendanceStatus(newStatus);
                                    } else {
                                        if (!student.getAttendanceStatus().equals("Not Marked")) {
                                            Log.d(TAG, "  ✓ Resetting " + student.getFullName() + " to Not Marked");
                                            updated = true;
                                        }
                                        student.setAttendanceStatus("Not Marked");
                                        student.setMarked(false);
                                        student.setTimestamp(null);
                                    }
                                } else {
                                    if (!student.getAttendanceStatus().equals("Not Marked")) {
                                        Log.d(TAG, "  ✓ No record for " + student.getFullName() + ", setting to Not Marked");
                                        updated = true;
                                    }
                                    student.setAttendanceStatus("Not Marked");
                                    student.setMarked(false);
                                    student.setTimestamp(null);
                                }
                            } else {
                                Log.w(TAG, "  ⚠ Student " + student.getEmail() + " has null ID, cannot update attendance");
                            }
                        }

                        if (updated) {
                            Log.d(TAG, "📱 Refreshing adapter with updates");
                            studentAdapter.notifyDataSetChanged();
                        } else {
                            Log.d(TAG, "No status changes detected");
                        }
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

        // Show/hide search bar with header
        View searchBarContainer = findViewById(R.id.searchBarContainer);
        if (searchBarContainer != null) {
            searchBarContainer.setVisibility(hasStudents ? View.VISIBLE : View.GONE);
            Log.d(TAG, "Search bar visibility set to: " + (hasStudents ? "VISIBLE" : "GONE"));
        } else {
            Log.e(TAG, "searchBarContainer is null!");
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

        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("allowedStudentEmails", FieldValue.arrayRemove(student.getEmail()))
                .addOnSuccessListener(unused -> {
                    db.collection("allClasses")
                            .document(classId)
                            .update("allowedStudentEmails", FieldValue.arrayRemove(student.getEmail()))
                            .addOnSuccessListener(unused2 -> {
                                decrementStudentCount(teacherId);

                                studentAdapter.removeStudent(position);
                                studentList.remove(student);
                                updateStudentsHeaderVisibility();

                                if (student.getStudentId() != null) {
                                    db.collection("allClasses")
                                            .document(classId)
                                            .collection("attendanceRecords")
                                            .document(student.getStudentId())
                                            .delete();

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
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("students", FieldValue.increment(-1))
                .addOnSuccessListener(unused -> {
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
        Log.d(TAG, "Setting up teacher view for class: " + classModel.getClassName());

        AttendanceCardBinding attendanceBinding = binding.attendanceCard;
        attendanceBinding.classCodeText.setText(classModel.getClassCode() != null ? classModel.getClassCode() : "N/A");

        isSessionActive = classModel.isAttendanceActive();
        updateTeacherAttendanceUI(attendanceBinding);
        Log.d(TAG, "Initial session state: " + (isSessionActive ? "ACTIVE" : "INACTIVE"));

        // THIS IS THE CRITICAL PART - Make sure the button click works
        attendanceBinding.attendanceButton.setOnClickListener(v -> {
            Log.d(TAG, "📱 Attendance button clicked!");
            Log.d(TAG, "Current session state before toggle: " + (isSessionActive ? "ACTIVE" : "INACTIVE"));

            // Toggle the session state
            isSessionActive = !isSessionActive;

            Log.d(TAG, "Session state after toggle: " + (isSessionActive ? "ACTIVE" : "INACTIVE"));

            // Update the UI
            updateTeacherAttendanceUI(attendanceBinding);

            // Update Firebase
            Log.d(TAG, "Calling updateAttendanceStatusInFirestore with isActive=" + isSessionActive);
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

            loadRecentSessions();
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

        setupRecentSessionsListener();
    }

    private void setupClassInfoListener() {
        if (classId == null || classId.isEmpty()) {
            Log.e(TAG, "Cannot setup listener - classId is null");
            return;
        }

        Log.d(TAG, "=== SETTING UP CLASS INFO LISTENER ===");
        Log.d(TAG, "Class ID: " + classId);

        // Remove old listener if exists
        if (classInfoListener != null) {
            classInfoListener.remove();
            Log.d(TAG, "Removed old listener");
        }

        classInfoListener = db.collection("allClasses")
                .document(classId)
                .addSnapshotListener((snapshot, error) -> {
                    Log.d(TAG, "🔔 CLASS INFO LISTENER FIRED");

                    if (error != null) {
                        Log.e(TAG, "ERROR in classInfoListener:", error);
                        return;
                    }

                    if (snapshot != null) {
                        Log.d(TAG, "Snapshot exists: " + snapshot.exists());

                        if (snapshot.exists()) {
                            Log.d(TAG, "========== SNAPSHOT DATA ==========");
                            Log.d(TAG, "All fields: " + snapshot.getData().keySet());

                            // Try different possible field names
                            Long studentCount = snapshot.getLong("students");
                            Log.d(TAG, "  students (Long): " + studentCount);

                            if (studentCount == null) {
                                Object studentsObj = snapshot.get("students");
                                Log.d(TAG, "  students (Object): " + studentsObj + " (class: " +
                                        (studentsObj != null ? studentsObj.getClass().getSimpleName() : "null") + ")");

                                // Try converting if it's a different type
                                if (studentsObj instanceof Number) {
                                    studentCount = ((Number) studentsObj).longValue();
                                }
                            }

                            Log.d(TAG, "==================================");

                            if (studentCount != null) {
                                String countText = String.valueOf(studentCount);
                                binding.classInfoCard.infoStudents.setText(countText);
                                Log.d(TAG, "✓✓✓ STUDENT COUNT UPDATED TO: " + countText);
                            } else {
                                Log.e(TAG, "✗✗✗ 'students' field is NULL or MISSING");
                                Log.e(TAG, "Check Firestore - field might be named differently");
                                // Fallback to original value from ClassModel
                                Log.d(TAG, "Keeping original student count from ClassModel");
                            }
                        } else {
                            Log.e(TAG, "✗ Snapshot exists but document is empty");
                        }
                    } else {
                        Log.e(TAG, "✗ Snapshot is NULL");
                    }
                });

        Log.d(TAG, "=== LISTENER ATTACHED ===");
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
        Log.d(TAG, "Updating teacher attendance UI - isSessionActive: " + isSessionActive);

        if (isSessionActive) {
            attendanceBinding.attendanceButton.setText("End Attendance Session");
            attendanceBinding.attendanceButton.setBackgroundResource(R.drawable.alt_attendance_button);
            attendanceBinding.bellIcon.setBackgroundResource(R.drawable.alt_attendance_button);
            attendanceBinding.classCodeCard.setCardBackgroundColor(0xFFFF5252);

            Log.d(TAG, "UI updated: Session is ACTIVE (End button shown)");
        } else {
            attendanceBinding.attendanceButton.setText("Start Attendance Session");
            attendanceBinding.attendanceButton.setBackgroundResource(R.drawable.attendance_button);
            attendanceBinding.bellIcon.setBackgroundResource(R.drawable.attendance_button);
            attendanceBinding.classCodeCard.setCardBackgroundColor(0xFF2EAD00);

            Log.d(TAG, "UI updated: Session is INACTIVE (Start button shown)");
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

        Log.d(TAG, "=== UPDATING ATTENDANCE STATUS ===");
        Log.d(TAG, "isActive: " + isActive);

        // Track session start time
        if (isActive) {
            attendanceSessionStartTime = System.currentTimeMillis();
            Log.d(TAG, "Session start time recorded: " + attendanceSessionStartTime);
        }

        // Update in user's classes collection
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("attendanceActive", isActive)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✓ Updated attendanceActive in user's classes");

                    // Update in allClasses collection
                    db.collection("allClasses")
                            .document(classId)
                            .update("attendanceActive", isActive)
                            .addOnSuccessListener(unused2 -> {
                                String message = isActive ? "Attendance session started!" : "Attendance session ended!";
                                Toast.makeText(ClassInformation.this, message, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "✓ Updated attendanceActive in allClasses");

                                // CRITICAL: Only save/clear if ENDING session
                                if (!isActive) {
                                    Log.d(TAG, "SESSION ENDING - saving and clearing records");

                                    // IMPORTANT: Save FIRST, then clear AFTER
                                    saveRecentSession(System.currentTimeMillis());

                                    // Add a small delay to ensure save completes before clearing
                                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                                            this::clearAllAttendanceRecords,
                                            500  // 500ms delay
                                    );
                                } else {
                                    Log.d(TAG, "SESSION STARTING - records will accumulate");
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update allClasses", e);
                                Toast.makeText(ClassInformation.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update attendance status in user's classes", e);
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearAllAttendanceRecords() {
        if (classId == null || classId.isEmpty()) return;

        Log.d(TAG, "Step 3: Clearing class-level attendance records...");

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int deleteCount = querySnapshot.size();
                    Log.d(TAG, "Found " + deleteCount + " records to clear");

                    if (deleteCount == 0) {
                        Log.d(TAG, "✅ No records to clear - refreshing sessions");
                        loadRecentSessions();
                        return;
                    }

                    final int[] deletedCount = {0};

                    for (var doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete()
                                .addOnSuccessListener(unused -> {
                                    deletedCount[0]++;
                                    Log.d(TAG, "  ✓ Deleted record " + deletedCount[0] + "/" + deleteCount);

                                    if (deletedCount[0] == deleteCount) {
                                        Log.d(TAG, "✅ All class-level records cleared");
                                        Log.d(TAG, "");
                                        Log.d(TAG, "╔════════════════════════════════════════╗");
                                        Log.d(TAG, "║    SESSION SAVE COMPLETE                ║");
                                        Log.d(TAG, "╚════════════════════════════════════════╝");
                                        Log.d(TAG, "");

                                        // Reset student UI
                                        for (StudentAttendanceModel student : studentList) {
                                            student.setAttendanceStatus("Not Marked");
                                            student.setMarked(false);
                                            student.setTimestamp(null);
                                        }
                                        studentAdapter.notifyDataSetChanged();

                                        // Reload sessions to show the new one
                                        loadRecentSessions();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting record: " + e.getMessage());
                                    deletedCount[0]++;

                                    if (deletedCount[0] == deleteCount) {
                                        Log.d(TAG, "Clearing completed (some deletes may have failed)");
                                        loadRecentSessions();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ CRITICAL ERROR: Failed to clear attendance records: " + e.getMessage());
                });
    }

    private void setupClassInfo(ClassModel classModel) {
        binding.classInfoCard.infoClassName.setText(classModel.getClassName() != null ? classModel.getClassName() : "N/A");
        binding.classInfoCard.infoClassCode.setText(classModel.getClassCode() != null ? classModel.getClassCode() : "N/A");
        binding.classInfoCard.infoSubjectCode.setText(classModel.getSubjectCode() != null ? classModel.getSubjectCode() : "N/A");
        binding.classInfoCard.infoStartTime.setText(classModel.getStartTime() != null ? classModel.getStartTime() : "N/A");
        binding.classInfoCard.infoEndTime.setText(classModel.getEndTime() != null ? classModel.getEndTime() : "N/A");
        binding.classInfoCard.infoRoom.setText(classModel.getRoom() != null ? classModel.getRoom() : "N/A");

        // Set initial student count
        binding.classInfoCard.infoStudents.setText(String.valueOf(classModel.getStudents()));
        Log.d(TAG, "Initial student count set to: " + classModel.getStudents());

        String classDays = classModel.getClassDays();
        if (classDays != null && !classDays.isEmpty()) {
            binding.classInfoCard.infoClassDays.setText(classDays);
        } else {
            binding.classInfoCard.infoClassDays.setText("N/A");
        }

        loadTeacherNameFromClassCreator(classModel.getTeacherId());
    }

    private void loadTeacherNameFromClassCreator(String teacherId) {
        if (teacherId == null || teacherId.isEmpty()) {
            binding.classInfoCard.infoTeacher.setText("N/A");
            return;
        }

        db.collection("users")
                .document(teacherId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String firstName = doc.getString("firstName");
                        String lastName = doc.getString("lastName");

                        String fullName = "N/A";
                        if (firstName != null && lastName != null) {
                            fullName = firstName + " " + lastName;
                        } else if (firstName != null) {
                            fullName = firstName;
                        }

                        binding.classInfoCard.infoTeacher.setText(fullName);
                        Log.d(TAG, "✓ Teacher name loaded: " + fullName);
                    } else {
                        binding.classInfoCard.infoTeacher.setText("N/A");
                    }
                })
                .addOnFailureListener(e -> {
                    binding.classInfoCard.infoTeacher.setText("N/A");
                    Log.e(TAG, "Error loading teacher", e);
                });
    }

    private void incrementStudentCount(String teacherId) {
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("students", FieldValue.increment(1))
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✓ Incremented student count in user's classes");
                    db.collection("allClasses")
                            .document(classId)
                            .update("students", FieldValue.increment(1))
                            .addOnSuccessListener(unused2 -> {
                                Log.d(TAG, "✓ Incremented student count in allClasses");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to increment count in allClasses", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to increment student count", e);
                });
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

    private void checkIfAllRecordsCopied(int savedCount, int failedCount, int totalCount) {
        int processedCount = savedCount + failedCount;

        if (processedCount >= totalCount) {
            Log.d(TAG, "");
            Log.d(TAG, "╔════════════════════════════════════════╗");
            Log.d(TAG, "║  ATTENDANCE RECORDS COPY COMPLETE      ║");
            Log.d(TAG, "╚════════════════════════════════════════╝");
            Log.d(TAG, "Saved: " + savedCount);
            Log.d(TAG, "Failed: " + failedCount);
            Log.d(TAG, "Total: " + totalCount);
            Log.d(TAG, "");

            // ✅ IMPORTANT: Verify records were copied BEFORE clearing
            verifyRecordsCopiedThenClear();
        }
    }

    private void verifyRecordsCopiedThenClear() {
        String sessionId = String.valueOf(System.currentTimeMillis());

        // Get the session ID from the most recent session
        db.collection("allClasses")
                .document(classId)
                .collection("recentSessions")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.e(TAG, "❌ No recent session found - cannot verify!");
                        clearAllAttendanceRecords();
                        return;
                    }

                    String actualSessionId = snapshot.getDocuments().get(0).getString("sessionId");
                    Log.d(TAG, "Verifying records for session: " + actualSessionId);

                    // Check how many records were copied
                    db.collection("allClasses")
                            .document(classId)
                            .collection("recentSessions")
                            .document(actualSessionId)
                            .collection("attendanceRecords")
                            .get()
                            .addOnSuccessListener(recordSnapshot -> {
                                Log.d(TAG, "✓ Verification: Found " + recordSnapshot.size() + " records in session");

                                if (recordSnapshot.size() > 0) {
                                    Log.d(TAG, "✅ Records successfully copied - now clearing class-level records");
                                    clearAllAttendanceRecords();
                                } else {
                                    Log.w(TAG, "⚠ No records copied - session might be empty");
                                    clearAllAttendanceRecords();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error verifying records: " + e.getMessage());
                                clearAllAttendanceRecords(); // Clear anyway
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting session: " + e.getMessage());
                    clearAllAttendanceRecords();
                });
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
        if (recentSessionsListener != null) {
            recentSessionsListener.remove();
        }
    }
}