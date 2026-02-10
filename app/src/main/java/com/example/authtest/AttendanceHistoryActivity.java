package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

// Shows students/teachers list of classes they've attended or created
public class AttendanceHistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private AttendanceHistoryAdapter historyAdapter;
    private List<AttendanceHistoryModel> historyList = new ArrayList<>();
    private List<AttendanceHistoryModel> filteredHistory = new ArrayList<>();
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private LinearLayout emptyStateLayout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isStudent = false;
    private EditText historySearchBar;
    private ImageView clearSearchButton;
    private LinearLayout searchBarContainer;

    /*
        Determines if user is student or teacher via email domain check
        Calls appropriate load method: loadStudentHistory() or loadTeacherHistory()
        Sets up search functionality and RecyclerView
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_history);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        historySearchBar = findViewById(R.id.historySearchBar);
        clearSearchButton = findViewById(R.id.clearSearchButton);
        searchBarContainer = findViewById(R.id.searchBarContainer);

        ImageView hamburgerIcon = findViewById(R.id.hamburger_icon);

        hamburgerIcon.setOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();
        setupRecyclerView();
        setupHistorySearch();
        checkUserTypeAndLoadHistory();
        checkUserTypeAndConfigureMenu();

        findViewById(R.id.backButton).setOnClickListener(view -> {
            finish();
        });
    }

    private void setupRecyclerView() {
        historyAdapter = new AttendanceHistoryAdapter(historyList);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(historyAdapter);
        historyRecyclerView.setNestedScrollingEnabled(true);
    }

    /*
        Implements TextWatcher on search EditText
        Filters history list based on className, classCode, or subjectCode matches
        Shows/hides clear button based on search query content
     */
    private void setupHistorySearch() {
        historySearchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchQuery = s.toString().trim().toLowerCase();
                filterHistory(searchQuery);

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
            historySearchBar.setText("");
            filteredHistory.clear();
            historyAdapter.updateList(new ArrayList<>(historyList));
        });
    }

    /*
        Creates new filtered list containing only matching items
        Updates adapter with filtered list
        Shows empty state layout if no results
     */
    private void filterHistory(String searchQuery) {
        filteredHistory.clear();

        if (searchQuery.isEmpty()) {
            historyAdapter.updateList(new ArrayList<>(historyList));
            return;
        }

        for (AttendanceHistoryModel history : historyList) {
            String className = history.getClassName() != null ? history.getClassName().toLowerCase() : "";
            String classCode = history.getClassCode() != null ? history.getClassCode().toLowerCase() : "";
            String subjectCode = history.getSubjectCode() != null ? history.getSubjectCode().toLowerCase() : "";

            if (className.contains(searchQuery) ||
                    classCode.contains(searchQuery) ||
                    subjectCode.contains(searchQuery)) {
                filteredHistory.add(history);
            }
        }

        historyAdapter.updateList(new ArrayList<>(filteredHistory));
    }

    private void checkUserTypeAndLoadHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String email = currentUser.getEmail();
        if (email != null && email.contains("@students.")) {
            isStudent = true;
            loadStudentHistory();
        } else {
            isStudent = false;
            loadTeacherHistory();
        }
    }

    /*
        Retrieves enrolledClasses subcollection from student's user document
        For each enrolled class, fetches full ClassModel from allClasses collection
        Uses counter pattern to track when all classes are loaded
        Sorts history by date descending after all loads complete
     */
    private void loadStudentHistory() {
        String studentId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(studentId)
                .collection("enrolledClasses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    historyList.clear();

                    if (querySnapshot.isEmpty()) {
                        updateUI(true);
                        return;
                    }

                    final int[] loadedCount = {0};
                    final int totalClasses = querySnapshot.size();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String classId = doc.getString("classId");
                        Long enrolledAt = doc.getLong("enrolledAt");

                        if (classId != null && !classId.isEmpty()) {
                            db.collection("allClasses")
                                    .document(classId)
                                    .get()
                                    .addOnSuccessListener(classDoc -> {
                                        if (classDoc.exists()) {
                                            ClassModel classModel = classDoc.toObject(ClassModel.class);
                                            if (classModel != null) {
                                                AttendanceHistoryModel historyModel = new AttendanceHistoryModel(
                                                        classId,
                                                        classModel.getClassName(),
                                                        classModel.getClassCode(),
                                                        classModel.getSubjectCode(),
                                                        classModel.getStartTime(),
                                                        classModel.getEndTime(),
                                                        enrolledAt != null ? enrolledAt : System.currentTimeMillis()
                                                );
                                                historyList.add(historyModel);
                                            }
                                        }

                                        loadedCount[0]++;
                                        if (loadedCount[0] == totalClasses) {
                                            sortHistoryByDate();
                                            historyAdapter.notifyDataSetChanged();
                                            updateUI(historyList.isEmpty());
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        loadedCount[0]++;
                                        if (loadedCount[0] == totalClasses) {
                                            sortHistoryByDate();
                                            historyAdapter.notifyDataSetChanged();
                                            updateUI(historyList.isEmpty());
                                        }
                                    });
                        } else {
                            loadedCount[0]++;
                            if (loadedCount[0] == totalClasses) {
                                sortHistoryByDate();
                                historyAdapter.notifyDataSetChanged();
                                updateUI(historyList.isEmpty());
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateUI(true);
                });
    }

    /*
        Retrieves classes subcollection from teacher's user document
        Directly maps document to ClassModel (no additional queries needed)
        Simpler than student path since teacher owns the class data directly
     */
    private void loadTeacherHistory() {
        String teacherId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    historyList.clear();

                    if (querySnapshot.isEmpty()) {
                        updateUI(true);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ClassModel classModel = doc.toObject(ClassModel.class);
                        if (classModel != null) {
                            Long createdAt = doc.getLong("createdAt");
                            if (createdAt == null) {
                                createdAt = System.currentTimeMillis();
                            }

                            AttendanceHistoryModel historyModel = new AttendanceHistoryModel(
                                    doc.getId(),
                                    classModel.getClassName(),
                                    classModel.getClassCode(),
                                    classModel.getSubjectCode(),
                                    classModel.getStartTime(),
                                    classModel.getEndTime(),
                                    createdAt
                            );
                            historyList.add(historyModel);
                        }
                    }

                    sortHistoryByDate();
                    historyAdapter.notifyDataSetChanged();
                    updateUI(historyList.isEmpty());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateUI(true);
                });
    }

    private void sortHistoryByDate() {
        historyList.sort((h1, h2) -> Long.compare(h2.getDateAdded(), h1.getDateAdded()));
    }

    private void updateUI(boolean isEmpty) {
        if (isEmpty) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            historyRecyclerView.setVisibility(View.GONE);
            searchBarContainer.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            historyRecyclerView.setVisibility(View.VISIBLE);
            searchBarContainer.setVisibility(View.VISIBLE);
        }
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
                startActivity(new Intent(AttendanceHistoryActivity.this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(AttendanceHistoryActivity.this, AttendanceStreak.class));
                return true;
            } else if (itemId == R.id.menu_leaderboards) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(AttendanceHistoryActivity.this, Leaderboards.class));
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(AttendanceHistoryActivity.this, ArchiveActivity.class));
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
                                startActivity(new Intent(AttendanceHistoryActivity.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(AttendanceHistoryActivity.this, TeacherHome.class));
                            } else {
                                Toast.makeText(AttendanceHistoryActivity.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(AttendanceHistoryActivity.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AttendanceHistoryActivity.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(AttendanceHistoryActivity.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(AttendanceHistoryActivity.this, SignIn.class);
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
}