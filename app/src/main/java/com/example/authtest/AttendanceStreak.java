package com.example.authtest;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.authtest.databinding.ActivityAttendanceStreakBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Displays student's 30-day attendance heat map (green = present, dark = absent/no class)
public class AttendanceStreak extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActivityAttendanceStreakBinding binding;
    private TextView longestStreakNumber;
    private List<CardView> streakCards = new ArrayList<>();
    private String userId;
    private static final String COLOR_BLACK = "#1A1A1A";
    private static final String COLOR_GREEN = "#26A641";
    private ListenerRegistration attendanceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        binding = ActivityAttendanceStreakBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.hamburgerIcon.setOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        binding.backButton.setOnClickListener(view -> {
            finish();
        });

        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();
        checkUserTypeAndConfigureMenu();
        initializeStreakViews();
        listenAttendanceData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (attendanceListener != null) {
            attendanceListener.remove();
        }
    }

    /*
        Creates array of 30 CardView references (5 weeks x 6 days/week)
        Each card represents one day in the grid
        IDs follow pattern: monday_week1, tuesday_week1, etc.
     */
    private void initializeStreakViews() {
        longestStreakNumber = findViewById(R.id.longestStreakNumber);
        initializeStreakCards();
    }

    private void initializeStreakCards() {
        streakCards.add(findViewById(R.id.monday_week1));
        streakCards.add(findViewById(R.id.tuesday_week1));
        streakCards.add(findViewById(R.id.wednesday_week1));
        streakCards.add(findViewById(R.id.thursday_week1));
        streakCards.add(findViewById(R.id.friday_week1));
        streakCards.add(findViewById(R.id.saturday_week1));

        streakCards.add(findViewById(R.id.monday_week2));
        streakCards.add(findViewById(R.id.tuesday_week2));
        streakCards.add(findViewById(R.id.wednesday_week2));
        streakCards.add(findViewById(R.id.thursday_week2));
        streakCards.add(findViewById(R.id.friday_week2));
        streakCards.add(findViewById(R.id.saturday_week2));

        streakCards.add(findViewById(R.id.monday_week3));
        streakCards.add(findViewById(R.id.tuesday_week3));
        streakCards.add(findViewById(R.id.wednesday_week3));
        streakCards.add(findViewById(R.id.thursday_week3));
        streakCards.add(findViewById(R.id.friday_week3));
        streakCards.add(findViewById(R.id.saturday_week3));

        streakCards.add(findViewById(R.id.monday_week4));
        streakCards.add(findViewById(R.id.tuesday_week4));
        streakCards.add(findViewById(R.id.wednesday_week4));
        streakCards.add(findViewById(R.id.thursday_week4));
        streakCards.add(findViewById(R.id.friday_week4));
        streakCards.add(findViewById(R.id.saturday_week4));

        streakCards.add(findViewById(R.id.monday_week5));
        streakCards.add(findViewById(R.id.tuesday_week5));
        streakCards.add(findViewById(R.id.wednesday_week5));
        streakCards.add(findViewById(R.id.thursday_week5));
        streakCards.add(findViewById(R.id.friday_week5));
        streakCards.add(findViewById(R.id.saturday_week5));
    }

    /*
        Calculates which Monday to start the 30-day grid from
        Goes back 29 days and finds the previous Monday
        Zeros out time components so dates are comparable
     */
    private Calendar getGridStartMonday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -29);

        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /*
        Queries attendance collection for current user
        Filters records by date range matching 30-day grid
        Creates HashMap mapping date string to attendance status
        Only keeps "present" status records (filters out late/absent)
     */
    private void listenAttendanceData() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        Calendar gridStart = getGridStartMonday();
        Calendar gridEnd = (Calendar) gridStart.clone();
        gridEnd.add(Calendar.DAY_OF_YEAR, 34);

        String startStr = dateFormat.format(gridStart.getTime());
        String endStr   = dateFormat.format(gridEnd.getTime());

        if (attendanceListener != null) {
            attendanceListener.remove();
        }

        attendanceListener = db.collection("attendance")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (queryDocumentSnapshots == null) return;

                    Map<String, String> attendanceMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String date   = doc.getString("date");
                        String status = doc.getString("status");
                        Boolean falseMarked = doc.getBoolean("falseMarked");

                        if (date == null) {
                            continue;
                        }

                        if (falseMarked != null && falseMarked) {
                            continue;
                        }

                        if (!"present".equals(status)) {
                            continue;
                        }

                        if (date.compareTo(startStr) >= 0 && date.compareTo(endStr) <= 0) {
                            if (!"present".equals(attendanceMap.get(date))) {
                                attendanceMap.put(date, status);
                            }
                        }
                    }

                    updateStreakDisplay(attendanceMap);
                });
    }

    /*
        Iterates through all 30 days in grid
        For each day, looks up attendance status in map
        Calculates longest current streak while iterating
        Updates card colors and streak display
     */
    private void updateStreakDisplay(Map<String, String> attendanceMap) {
        Calendar calendar = getGridStartMonday();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        int longestStreak = 0;
        int tempStreak    = 0;
        int cardIndex     = 0;

        for (int week = 0; week < 5; week++) {
            for (int day = 0; day < 6; day++) {
                String date   = dateFormat.format(calendar.getTime());
                String status = attendanceMap.getOrDefault(date, "no_class");

                if (cardIndex < streakCards.size() && streakCards.get(cardIndex) != null) {
                    updateCardColor(streakCards.get(cardIndex), status);
                }

                cardIndex++;

                if ("present".equals(status)) {
                    tempStreak++;
                    if (tempStreak > longestStreak) longestStreak = tempStreak;
                } else {
                    tempStreak = 0;
                }

                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        longestStreakNumber.setText(String.valueOf(longestStreak));
    }

    /*
        Sets card background to green (#26A641) if "present"
        Sets card background to dark (#1A1A1A) if not present or no class
     */
    private void updateCardColor(CardView card, String status) {
        card.setCardBackgroundColor(
                "present".equals(status)
                        ? Color.parseColor(COLOR_GREEN)
                        : Color.parseColor(COLOR_BLACK)
        );
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
                startActivity(new Intent(AttendanceStreak.this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.menu_leaderboards) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(AttendanceStreak.this, Leaderboards.class));
                return true;
            } else if (itemId == R.id.menu_attendance_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(AttendanceStreak.this, AttendanceHistoryActivity.class));
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(AttendanceStreak.this, ArchiveActivity.class));
                return true;
            } else if (itemId == R.id.menu_settings) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(AttendanceStreak.this, SettingsActivity.class));
                return true;
            } else if (itemId == R.id.menu_logout) {
                LogoutConfirmationDialog confirmDialog = new LogoutConfirmationDialog(this, this::logout,
                        () -> drawerLayout.closeDrawer(GravityCompat.START)
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
                                startActivity(new Intent(AttendanceStreak.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(AttendanceStreak.this, TeacherHome.class));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(AttendanceStreak.this, "Error loading user info", Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(AttendanceStreak.this, SignIn.class);
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
                            String lastName  = documentSnapshot.getString("lastName");

                            String fullName  = "";
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
        if (currentUser == null) return;

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