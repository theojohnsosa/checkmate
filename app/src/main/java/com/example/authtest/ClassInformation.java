package com.example.authtest;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

public class ClassInformation extends AppCompatActivity {

    private static final String TAG = "ClassInformation";
    private AppCompatButton viewSeatPlanButton;
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

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        binding.hamburgerIcon.setOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        studentSearchBar = findViewById(R.id.studentSearchBar);
        clearSearchButton = findViewById(R.id.clearSearchButton);

        setupStudentSearch();
        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();
        checkUserTypeAndConfigureMenu();

        binding.backButton.setOnClickListener(view -> {
            finish();
        });

        addStudentsButton = findViewById(R.id.addStudentsButton);

        View statsCard = binding.attendanceStatsCard.getRoot();
        totalStudentsCount = statsCard.findViewById(R.id.totalStudentsCount);
        presentCount = statsCard.findViewById(R.id.presentCount);
        lateCount = statsCard.findViewById(R.id.lateCount);
        absentCount = statsCard.findViewById(R.id.absentCount);

        studentsRecyclerView = findViewById(R.id.studentsRecyclerView);
        studentsAttendedHeader = findViewById(R.id.studentsAttendedHeader);
        recentSessionsRecyclerView = findViewById(R.id.recentSessionsRecyclerView);

        checkUserTypeAndSetupUI();

        setupRecentSessionsRecyclerView();

        viewSeatPlanButton = findViewById(R.id.viewSeatPlanButton);
        viewSeatPlanButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, SeatPlan.class);
            intent.putExtra("CLASS_ID", classId);
            startActivity(intent);
        });

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("CLASS_MODEL")) {
            ClassModel classModel = (ClassModel) intent.getSerializableExtra("CLASS_MODEL");

            if (classModel != null) {
                if (intent.hasExtra("CLASS_ID")) {
                    classId = intent.getStringExtra("CLASS_ID");
                } else {
                    classId = intent.getStringExtra("CLASS_CODE");
                }

                if (classId == null || classId.isEmpty()) {
                    queryClassIdByCode(classModel.getClassCode(), classModel);
                    return;
                }

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
                    setupStudentListRecyclerView();
                    loadStudentList();
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
            addStudentsButton.setOnClickListener(view -> {
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

    /**
     * Query Firestore to find the document ID by classCode
     */
    private void queryClassIdByCode(String classCode, ClassModel classModel) {
        if (classCode == null || classCode.isEmpty()) {
            Toast.makeText(this, "Invalid class code", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("allClasses")
                .whereEqualTo("classCode", classCode)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        classId = snapshot.getDocuments().get(0).getId();
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
                            setupStudentListRecyclerView();
                            loadStudentList();
                        }

                        loadRecentSessions();
                        setupClassInfo(classModel);
                    } else {
                        Toast.makeText(ClassInformation.this, "Class not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ClassInformation.this, "Error loading class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupRecentSessionsRecyclerView() {
        if (recentSessionsRecyclerView == null) {
            recentSessionsRecyclerView = findViewById(R.id.recentSessionsRecyclerView);
        }

        recentSessionAdapter = new RecentSessionAdapter();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        recentSessionsRecyclerView.setLayoutManager(layoutManager);
        recentSessionsRecyclerView.setAdapter(recentSessionAdapter);
        recentSessionsRecyclerView.setNestedScrollingEnabled(true);
        recentSessionsRecyclerView.setHasFixedSize(false);

        recentSessionAdapter.setClickListener(session -> {
            navigateToSessionDetails(session);
        });

        if (!isStudent) {
            recentSessionAdapter.setRemoveListener((session, position) -> {
                removeSessionFromClass(session, position);
            });
            setupSwipeToDeleteSessions();
        }
    }

    private void checkUserTypeAndConfigureMenu() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");

                        if (userType != null && !"Student".equalsIgnoreCase(userType.trim())) {
                            navigationView.getMenu().findItem(R.id.menu_streak).setVisible(false);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    navigationView.getMenu().findItem(R.id.menu_streak).setVisible(false);
                });
    }

    private void setupSwipeToDeleteSessions() {
        if (isStudent) {
            return;
        }

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
        if (isStudent) {
            Toast.makeText(this, "Only teachers can remove sessions", Toast.LENGTH_SHORT).show();
            return;
        }

        if (classId == null || session.getSessionId() == null) {
            return;
        }

        String sessionId = session.getSessionId();
        String teacherId = mAuth.getCurrentUser().getUid();

        db.collection("allClasses")
                .document(classId)
                .collection("recentSessions")
                .document(sessionId)
                .delete()
                .addOnSuccessListener(unused -> {
                    db.collection("users")
                            .document(teacherId)
                            .collection("classes")
                            .document(classId)
                            .collection("recentSessions")
                            .document(sessionId)
                            .delete()
                            .addOnSuccessListener(unused2 -> {
                                try {
                                    if (position >= 0 && position < recentSessionList.size()) {
                                        recentSessionList.remove(position);
                                        recentSessionAdapter.notifyItemRemoved(position);
                                        recentSessionAdapter.notifyItemRangeChanged(position, recentSessionList.size());
                                    } else {
                                        loadRecentSessions();
                                        return;
                                    }

                                    if (recentSessionList.isEmpty()) {
                                        updateRecentSessionsVisibility();
                                    }

                                    Toast.makeText(ClassInformation.this, "Session deleted successfully", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    loadRecentSessions();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ClassInformation.this, "Failed to delete session", Toast.LENGTH_SHORT).show();
                                try {
                                    if (position >= 0 && position < recentSessionList.size()) {
                                        recentSessionAdapter.notifyItemChanged(position);
                                    }
                                } catch (Exception ex) {

                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ClassInformation.this, "Failed to delete session", Toast.LENGTH_SHORT).show();
                    try {
                        if (position >= 0 && position < recentSessionList.size()) {
                            recentSessionAdapter.notifyItemChanged(position);
                        }
                    } catch (Exception ex) {

                    }
                });
    }

    private void loadRecentSessions() {
        if (classId == null || classId.isEmpty()) {
            return;
        }

        db.collection("allClasses")
                .document(classId)
                .collection("recentSessions")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    recentSessionList.clear();
                    for (var doc : querySnapshot.getDocuments()) {
                        long startTime = doc.getLong("sessionStartTime") != null ? doc.getLong("sessionStartTime") : 0;
                        long endTime = doc.getLong("sessionEndTime") != null ? doc.getLong("sessionEndTime") : 0;
                        String sessionId = doc.getString("sessionId");

                        RecentSession session = new RecentSession(sessionId, classId, startTime, endTime);
                        recentSessionList.add(session);
                    }

                    if (recentSessionAdapter == null) {
                        return;
                    }

                    List<RecentSession> adapterList = new ArrayList<>(recentSessionList);
                    recentSessionAdapter.setSessions(adapterList);
                    updateRecentSessionsVisibility();
                });
    }

    private void setupRecentSessionsListener() {
        if (classId == null || classId.isEmpty()) {
            return;
        }

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
                        return;
                    }

                    if (snapshots != null) {
                        recentSessionList.clear();

                        for (var doc : snapshots.getDocuments()) {
                            long startTime = doc.getLong("sessionStartTime") != null ? doc.getLong("sessionStartTime") : 0;
                            long endTime = doc.getLong("sessionEndTime") != null ? doc.getLong("sessionEndTime") : 0;
                            String sessionId = doc.getString("sessionId");

                            RecentSession session = new RecentSession(sessionId, classId, startTime, endTime);
                            recentSessionList.add(session);
                        }

                        if (recentSessionAdapter != null) {
                            recentSessionAdapter.setSessions(new ArrayList<>(recentSessionList));
                        }

                        updateRecentSessionsVisibility();
                    }
                });
    }

    private void updateRecentSessionsVisibility() {
        boolean hasSessions = !recentSessionList.isEmpty();

        View recentSessionsHeader = findViewById(R.id.recentSessionsHeader);
        View recentSessionsRecycler = findViewById(R.id.recentSessionsRecyclerView);

        if (recentSessionsHeader != null) {
            recentSessionsHeader.setVisibility(hasSessions ? View.VISIBLE : View.GONE);
        }

        if (recentSessionsRecycler != null) {
            recentSessionsRecycler.setVisibility(hasSessions ? View.VISIBLE : View.GONE);
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

                if (searchQuery.isEmpty()) {
                    clearSearchButton.setVisibility(View.GONE);
                } else {
                    clearSearchButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        clearSearchButton.setOnClickListener(view -> {
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

            if (fullName.contains(searchQuery) || firstName.contains(searchQuery) || lastName.contains(searchQuery)) {
                filteredStudentList.add(student);
            }
        }

        studentAdapter.setStudents(new ArrayList<>(filteredStudentList));
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
                startActivity(new Intent(ClassInformation.this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(ClassInformation.this, AttendanceStreak.class));
                return true;
            } else if (itemId == R.id.menu_leaderboards) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(ClassInformation.this, Leaderboards.class));
                return true;
            } else if (itemId == R.id.menu_attendance_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(ClassInformation.this, AttendanceHistoryActivity.class));
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(ClassInformation.this, ArchiveActivity.class));
                return true;
            } else if (itemId == R.id.menu_settings) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            } else if (itemId == R.id.menu_logout) {
                LogoutConfirmationDialog confirmDialog = new LogoutConfirmationDialog(this, this::logout,
                        () -> {
                            drawerLayout.closeDrawer(GravityCompat.START);
                        }
                );
                confirmDialog.show();
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

                        if (allowedEmails != null && !allowedEmails.isEmpty()) {
                            String targetEmail = allowedEmails.get(0);

                            db.collection("users")
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        for (var doc : querySnapshot.getDocuments()) {
                                            String schoolEmail = doc.getString("schoolEmail");
                                            String email = doc.getString("email");
                                            String userEmail = doc.getString("userEmail");
                                            String studentEmail = doc.getString("studentEmail");
                                            String emailAddress = doc.getString("emailAddress");

                                            String cleanTarget = targetEmail.toLowerCase().trim();

                                            if ((schoolEmail != null && schoolEmail.toLowerCase().trim().equals(cleanTarget)) ||
                                                    (email != null && email.toLowerCase().trim().equals(cleanTarget)) ||
                                                    (userEmail != null && userEmail.toLowerCase().trim().equals(cleanTarget)) ||
                                                    (studentEmail != null && studentEmail.toLowerCase().trim().equals(cleanTarget)) ||
                                                    (emailAddress != null && emailAddress.toLowerCase().trim().equals(cleanTarget))) {
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadStudentList();
        }
    }

    private void setupStudentListRecyclerView() {
        studentAdapter = new StudentAttendanceAdapter(classId, this::removeStudentFromClass);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        studentsRecyclerView.setLayoutManager(layoutManager);
        studentsRecyclerView.setAdapter(studentAdapter);

        studentsRecyclerView.setNestedScrollingEnabled(true);
        studentsRecyclerView.setHasFixedSize(false);

        if (!isStudent) {
            setupSwipeToDelete();
        }
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
            return;
        }

        db.collection("allClasses")
                .document(classId)
                .get()
                .addOnSuccessListener(classDoc -> {
                    if (classDoc.exists()) {
                        List<String> allowedEmails = (List<String>) classDoc.get("allowedStudentEmails");

                        if (allowedEmails == null || allowedEmails.isEmpty()) {
                            studentList.clear();
                            studentAdapter.setStudents(studentList);
                            updateStudentsHeaderVisibility();
                            return;
                        }

                        studentList.clear();
                        final int[] loadedCount = {0};
                        final int totalEmails = allowedEmails.size();

                        for (String email : allowedEmails) {
                            String cleanEmail = email.toLowerCase().trim();

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

                                            StudentAttendanceModel student = new StudentAttendanceModel(
                                                    studentId,
                                                    userEmail != null ? userEmail : email,
                                                    firstName != null ? firstName : "Unknown",
                                                    lastName != null ? lastName : "User"
                                            );

                                            studentList.add(student);
                                            checkStudentAttendance(student);
                                        } else {
                                            queryAlternativeEmail(cleanEmail, email);
                                        }

                                        loadedCount[0]++;
                                        checkIfAllLoaded(loadedCount[0], totalEmails);
                                    })
                                    .addOnFailureListener(e -> {
                                        StudentAttendanceModel student = new StudentAttendanceModel(
                                                null, email, "Unknown", "User"
                                        );
                                        student.setAttendanceStatus("Not Marked");
                                        studentList.add(student);

                                        loadedCount[0]++;
                                        checkIfAllLoaded(loadedCount[0], totalEmails);
                                    });
                        }
                    }
                });
    }

    private void queryAlternativeEmail(String cleanEmail, String email) {
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

                        StudentAttendanceModel student = new StudentAttendanceModel(
                                studentId,
                                userEmail != null ? userEmail : email,
                                firstName != null ? firstName : "Unknown",
                                lastName != null ? lastName : "User"
                        );

                        studentList.add(student);
                        checkStudentAttendance(student);
                    } else {
                        StudentAttendanceModel student = new StudentAttendanceModel(
                                null, email, "Unknown", "User"
                        );
                        student.setAttendanceStatus("Not Marked");
                        studentList.add(student);
                    }
                })
                .addOnFailureListener(e -> {
                    StudentAttendanceModel student = new StudentAttendanceModel(
                            null, email, "Unknown", "User"
                    );
                    student.setAttendanceStatus("Not Marked");
                    studentList.add(student);
                });
    }

    private void checkIfAllLoaded(int loadedCount, int totalEmails) {
        if (loadedCount == totalEmails) {
            sortStudentsByLastName();

            filteredStudentList.clear();
            filteredStudentList.addAll(studentList);

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
    }

    private void checkStudentAttendance(StudentAttendanceModel student) {
        if (classId == null || student.getStudentId() == null) {
            student.setAttendanceStatus("Not Marked");
            student.setMarked(false);
            student.setTimestamp(null);
            return;
        }

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .document(student.getStudentId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean marked = doc.getBoolean("marked");
                        Long timestamp = doc.getLong("timestamp");

                        if (marked != null && marked && timestamp != null) {
                            student.setMarked(true);
                            student.setTimestamp(timestamp);
                            String status = getAttendanceStatus(timestamp);
                            student.setAttendanceStatus(status);
                        } else {
                            student.setAttendanceStatus("Not Marked");
                            student.setMarked(false);
                            student.setTimestamp(null);
                        }
                    } else {
                        student.setAttendanceStatus("Not Marked");
                        student.setMarked(false);
                        student.setTimestamp(null);
                    }

                    studentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    student.setAttendanceStatus("Not Marked");
                    student.setMarked(false);
                    student.setTimestamp(null);
                    studentAdapter.notifyDataSetChanged();
                });
    }

    private void setupStudentsListener() {
        if (classId == null || classId.isEmpty()) {
            return;
        }

        if (studentsListener != null) {
            studentsListener.remove();
        }

        studentsListener = db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (snapshots != null) {
                        java.util.Map<String, com.google.firebase.firestore.DocumentSnapshot> attendanceMap = new java.util.HashMap<>();
                        for (var doc : snapshots.getDocuments()) {
                            attendanceMap.put(doc.getId(), doc);
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
                                            updated = true;
                                        }

                                        student.setMarked(true);
                                        student.setTimestamp(timestamp);
                                        student.setAttendanceStatus(newStatus);
                                    } else {
                                        if (!student.getAttendanceStatus().equals("Not Marked")) {
                                            updated = true;
                                        }
                                        student.setAttendanceStatus("Not Marked");
                                        student.setMarked(false);
                                        student.setTimestamp(null);
                                    }
                                } else {
                                    if (!student.getAttendanceStatus().equals("Not Marked")) {
                                        updated = true;
                                    }
                                    student.setAttendanceStatus("Not Marked");
                                    student.setMarked(false);
                                    student.setTimestamp(null);
                                }
                            }
                        }

                        if (updated) {
                            studentAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void updateStudentsHeaderVisibility() {
        boolean hasStudents = !studentList.isEmpty();

        if (studentsAttendedHeader != null) {
            studentsAttendedHeader.setVisibility(hasStudents ? View.VISIBLE : View.GONE);
        }

        View searchBarContainer = findViewById(R.id.searchBarContainer);

        if (searchBarContainer != null) {
            searchBarContainer.setVisibility(hasStudents ? View.VISIBLE : View.GONE);
        }

        if (studentsRecyclerView != null) {
            studentsRecyclerView.setVisibility(hasStudents ? View.VISIBLE : View.GONE);
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
                                Toast.makeText(this, "Failed to remove student", Toast.LENGTH_SHORT).show();
                                studentAdapter.notifyItemChanged(position);
                            });
                })
                .addOnFailureListener(e -> {
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
                            .update("students", FieldValue.increment(-1));
                });
    }

    private void setupTeacherView(ClassModel classModel) {
        AttendanceCardBinding attendanceBinding = binding.attendanceCard;
        attendanceBinding.classCodeText.setText(classModel.getClassCode() != null ? classModel.getClassCode() : "N/A");

        isSessionActive = classModel.isAttendanceActive();
        updateTeacherAttendanceUI(attendanceBinding);

        attendanceBinding.attendanceButton.setOnClickListener(view -> {
            isSessionActive = !isSessionActive;
            updateTeacherAttendanceUI(attendanceBinding);
            updateAttendanceStatusInFirestore(isSessionActive);
        });
    }

    private void setupStudentView(ClassModel classModel) {
        StudentsAttendanceStatusCardBinding studentCard = binding.studentAttendanceCard;

        studentCard.markAttendanceButton.setOnClickListener(view -> {
            markAttendance();
        });

        checkExistingAttendance();

        if (classId != null && !classId.isEmpty()) {
            DocumentReference classRef = db.collection("allClasses").document(classId);

            attendanceListener = classRef.addSnapshotListener((snapshot, error) -> {
                if (error != null) {
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
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Long studentCount = snapshot.getLong("students");
                        if (studentCount != null) {
                            totalStudentsCount.setText(String.valueOf(studentCount));
                        }

                        updateAttendanceStats();
                    }
                });

        setupRecentSessionsListener();
    }

    private void updateAttendanceStats() {
        int present = 0;
        int late = 0;
        int absent = 0;

        for (StudentAttendanceModel student : studentList) {
            if (student.isMarked() && student.getTimestamp() != null) {
                String status = student.getAttendanceStatus();
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

        presentCount.setText(String.valueOf(present));
        lateCount.setText(String.valueOf(late));
        absentCount.setText(String.valueOf(absent));
    }

    private void setupClassInfoListener() {
        if (classId == null || classId.isEmpty()) {
            return;
        }

        if (classInfoListener != null) {
            classInfoListener.remove();
        }

        classInfoListener = db.collection("allClasses")
                .document(classId)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        return;
                    }

                    if (snapshot != null) {
                        if (snapshot.exists()) {
                            Long studentCount = snapshot.getLong("students");

                            if (studentCount == null) {
                                Object studentsObj = snapshot.get("students");

                                if (studentsObj instanceof Number) {
                                    studentCount = ((Number) studentsObj).longValue();
                                }
                            }

                            if (studentCount != null) {
                                String countText = String.valueOf(studentCount);
                                binding.classInfoCard.infoStudents.setText(countText);
                            }
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

            if (differenceMinutes <= 15) {
                return "Present";
            } else if (differenceMinutes <= 30) {
                return "Late";
            } else {
                return "Absent";
            }

        } catch (ParseException e) {
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
                });
    }

    private void markAttendance() {
        if (classId == null || classId.isEmpty()) {
            Toast.makeText(this, "Error: Class ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isCurrentTimeWithinClassTime()) {
            Toast.makeText(this, "Attendance can only be marked during class time", Toast.LENGTH_SHORT).show();
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
                    saveAttendanceForStreak(studentId, timestamp);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to mark attendance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveAttendanceForStreak(String studentId, long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date(timestamp));

        db.collection("attendance")
                .whereEqualTo("userId", studentId)
                .whereEqualTo("date", today)
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Map<String, Object> streakAttendance = new HashMap<>();
                        streakAttendance.put("userId", studentId);
                        streakAttendance.put("date", today);
                        streakAttendance.put("status", "present");
                        streakAttendance.put("timestamp", timestamp);
                        streakAttendance.put("classId", classId);

                        db.collection("attendance")
                                .add(streakAttendance);
                    }
                });
    }

    private boolean isCurrentTimeWithinClassTime() {
        if (classStartTime == null || classStartTime.isEmpty()) {
            return false;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault());

            Date now = new Date();
            String todayDate = dateFormat.format(now);

            String startTimeString = todayDate + " " + classStartTime;
            Date classStartDateTime = fullFormat.parse(startTimeString);

            if (classStartDateTime == null) {
                return false;
            }

            long currentTimeMs = System.currentTimeMillis();
            long classStartTimeMs = classStartDateTime.getTime();

            return currentTimeMs >= classStartTimeMs;

        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
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

        if (isActive) {
            attendanceSessionStartTime = System.currentTimeMillis();
        }

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
                                Toast.makeText(ClassInformation.this, message, Toast.LENGTH_SHORT).show();

                                if (!isActive) {
                                    saveRecentSessionWithRecords(System.currentTimeMillis());
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ClassInformation.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveRecentSessionWithRecords(long endTime) {
        if (classId == null || classId.isEmpty()) {
            return;
        }

        long startTime = attendanceSessionStartTime > 0 ? attendanceSessionStartTime : (endTime - 3600000);
        String sessionId = String.valueOf(System.currentTimeMillis());

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int recordCount = querySnapshot.size();

                    if (recordCount == 0) {
                        createSessionDocument(sessionId, startTime, endTime, 0);
                        return;
                    }

                    final int[] savedCount = {0};
                    final int[] failedCount = {0};

                    for (var doc : querySnapshot.getDocuments()) {
                        String studentId = doc.getId();
                        Long timestamp = doc.getLong("timestamp");
                        Boolean marked = doc.getBoolean("marked");

                        if (timestamp != null) {
                            boolean isMarked = marked != null && marked;

                            Map<String, Object> recordData = new HashMap<>();
                            recordData.put("studentId", studentId);
                            recordData.put("timestamp", timestamp);
                            recordData.put("marked", isMarked);

                            db.collection("allClasses")
                                    .document(classId)
                                    .collection("recentSessions")
                                    .document(sessionId)
                                    .collection("attendanceRecords")
                                    .document(studentId)
                                    .set(recordData)
                                    .addOnSuccessListener(unused -> {
                                        savedCount[0]++;

                                        if ((savedCount[0] + failedCount[0]) == recordCount) {
                                            createSessionDocument(sessionId, startTime, endTime, savedCount[0]);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        failedCount[0]++;

                                        if ((savedCount[0] + failedCount[0]) == recordCount) {
                                            createSessionDocument(sessionId, startTime, endTime, savedCount[0]);
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ClassInformation.this, "Failed to save session", Toast.LENGTH_LONG).show();
                });
    }

    private void createSessionDocument(String sessionId, long startTime, long endTime, int recordCount) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", sessionId);
        sessionData.put("classId", classId);
        sessionData.put("sessionStartTime", startTime);
        sessionData.put("sessionEndTime", endTime);
        sessionData.put("timestamp", endTime);

        db.collection("allClasses")
                .document(classId)
                .collection("recentSessions")
                .document(sessionId)
                .set(sessionData)
                .addOnSuccessListener(unused -> {
                    String teacherId = mAuth.getCurrentUser().getUid();
                    db.collection("users")
                            .document(teacherId)
                            .collection("classes")
                            .document(classId)
                            .collection("recentSessions")
                            .document(sessionId)
                            .set(sessionData)
                            .addOnSuccessListener(unused2 -> {
                                clearAllAttendanceRecords();
                            })
                            .addOnFailureListener(e -> {
                                clearAllAttendanceRecords();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ClassInformation.this, "Failed to save session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearAllAttendanceRecords() {
        if (classId == null || classId.isEmpty()) return;

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int deleteCount = querySnapshot.size();

                    if (deleteCount == 0) {
                        for (StudentAttendanceModel student : studentList) {
                            student.setAttendanceStatus("Not Marked");
                            student.setMarked(false);
                            student.setTimestamp(null);
                        }
                        studentAdapter.notifyDataSetChanged();

                        loadRecentSessions();
                        return;
                    }

                    final int[] deletedCount = {0};

                    for (var doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete()
                                .addOnSuccessListener(unused -> {
                                    deletedCount[0]++;

                                    if (deletedCount[0] == deleteCount) {
                                        for (StudentAttendanceModel student : studentList) {
                                            student.setAttendanceStatus("Not Marked");
                                            student.setMarked(false);
                                            student.setTimestamp(null);
                                        }
                                        studentAdapter.notifyDataSetChanged();

                                        loadRecentSessions();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    deletedCount[0]++;

                                    if (deletedCount[0] == deleteCount) {
                                        loadRecentSessions();
                                    }
                                });
                    }
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
                    } else {
                        binding.classInfoCard.infoTeacher.setText("N/A");
                    }
                })
                .addOnFailureListener(e -> {
                    binding.classInfoCard.infoTeacher.setText("N/A");
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
            } else {
                isStudent = false;
                binding.attendanceCard.getRoot().setVisibility(View.VISIBLE);
                binding.studentAttendanceCard.getRoot().setVisibility(View.GONE);
                binding.attendanceStatsCard.getRoot().setVisibility(View.VISIBLE);

                if (addStudentsButton != null) {
                    addStudentsButton.setVisibility(View.VISIBLE);
                }

                if (viewSeatPlanButton != null) {
                    viewSeatPlanButton.setVisibility(View.VISIBLE);
                }
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

        if (recentSessionsListener != null) {
            recentSessionsListener.remove();
        }
    }
}