package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Shows ranking of students by attendance points
public class Leaderboards extends AppCompatActivity {

    private AppCompatButton backButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView userRankingName;
    private TextView userRankingPoints;
    private TextView userRankingPosition;
    private LinearLayout rankingListContainer;
    private LinearLayout podiumContainer;
    private ListenerRegistration attendanceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboards);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        findViewById(R.id.hamburger_icon).setOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();
        initializeViews();
        setupBackButton();
        loadCurrentUserInfo();
        listenLeaderboardData();
        checkUserTypeAndConfigureMenu();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (attendanceListener != null) {
            attendanceListener.remove();
        }
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        userRankingName = findViewById(R.id.userRankingName);
        userRankingPoints = findViewById(R.id.userRankingPoints);
        rankingListContainer = findViewById(R.id.rankingListContainer);
        podiumContainer = findViewById(R.id.podiumContainer);

        View parent = (View) userRankingName.getParent();
        if (parent instanceof LinearLayout) {
            LinearLayout container = (LinearLayout) parent;
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child instanceof TextView && child != userRankingName && child != userRankingPoints) {
                    userRankingPosition = (TextView) child;
                    break;
                }
            }
        }
    }

    private void setupBackButton() {
        backButton.setOnClickListener(view -> finish());
    }

    private void loadCurrentUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String fullName = ((firstName != null ? firstName : "") +
                                (lastName != null ? " " + lastName : "")).trim();
                        userRankingName.setText(fullName.isEmpty() ? "User" : fullName);
                    } else {
                        userRankingName.setText("User");
                    }
                    userRankingPoints.setText("0 pts");
                    if (userRankingPosition != null) userRankingPosition.setText("-");
                })
                .addOnFailureListener(e -> {
                    userRankingName.setText("User");
                    userRankingPoints.setText("0 pts");
                    if (userRankingPosition != null) userRankingPosition.setText("-");
                });
    }

    /*
         Sets up real-time listener on attendance collection (replaces one-time .get())
         Whenever any attendance record changes (including falseMarked flag), leaderboard recalculates
         Filters to only "present" status records that are NOT falseMarked
         Builds map of attendance by class/date to find early-bird students
     */
    private void listenLeaderboardData() {
        if (attendanceListener != null) {
            attendanceListener.remove();
        }

        attendanceListener = db.collection("attendance")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load leaderboard: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        fetchAllStudents(new HashMap<>());
                        return;
                    }

                    if (queryDocumentSnapshots == null) return;

                    Map<String, List<AttendanceRecord>> attendanceByClass = new HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId   = doc.getString("userId");
                        String status   = doc.getString("status");
                        String date     = doc.getString("date");
                        String classId  = doc.getString("classId");
                        Long timestamp  = doc.getLong("timestamp");
                        Boolean falseMarked = doc.getBoolean("falseMarked");

                        if (userId == null || !"present".equals(status) || date == null) continue;
                        if (falseMarked != null && falseMarked) continue;

                        String classKey = date + "_" + (classId != null ? classId : "default");

                        AttendanceRecord record = new AttendanceRecord();
                        record.userId = userId;
                        record.timestamp = timestamp != null ? timestamp : Long.MAX_VALUE;

                        attendanceByClass.putIfAbsent(classKey, new ArrayList<>());
                        attendanceByClass.get(classKey).add(record);
                    }

                    Map<String, LeaderboardEntry> userPointsMap = new HashMap<>();

                    for (List<AttendanceRecord> records : attendanceByClass.values()) {
                        if (records.isEmpty()) continue;

                        Collections.sort(records, (a, b) -> Long.compare(a.timestamp, b.timestamp));

                        AttendanceRecord earlyBird = records.get(0);

                        LeaderboardEntry entry = userPointsMap.get(earlyBird.userId);
                        if (entry == null) {
                            entry = new LeaderboardEntry();
                            entry.userId = earlyBird.userId;
                            entry.points = 0;
                            entry.earliestTimestamp = Long.MAX_VALUE;
                            userPointsMap.put(earlyBird.userId, entry);
                        }

                        entry.points += 1;

                        if (earlyBird.timestamp < entry.earliestTimestamp) {
                            entry.earliestTimestamp = earlyBird.timestamp;
                        }
                    }

                    fetchAllStudents(userPointsMap);
                });
    }

    /*
         Queries all Student users from database
         Combines with point map to create complete leaderboard
         Even students with 0 points appear in ranking
         Calls fetchUserNamesAndDisplay() to show full names
     */
    private void fetchAllStudents(Map<String, LeaderboardEntry> userPointsMap) {
        db.collection("users")
                .whereEqualTo("userType", "Student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<LeaderboardEntry> allEntries = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId    = doc.getId();
                        String firstName = doc.getString("firstName");
                        String lastName  = doc.getString("lastName");
                        String fullName  = ((firstName != null ? firstName : "") +
                                (lastName != null ? " " + lastName : "")).trim();

                        if (fullName.isEmpty()) fullName = "Unknown User";

                        LeaderboardEntry entry = userPointsMap.get(userId);
                        if (entry == null) {
                            entry = new LeaderboardEntry();
                            entry.userId = userId;
                            entry.points = 0;
                            entry.earliestTimestamp = Long.MAX_VALUE;
                        }
                        entry.fullName = fullName;
                        allEntries.add(entry);
                    }

                    if (!allEntries.isEmpty()) {
                        displayLeaderboard(allEntries);
                        loadUserRanking(allEntries);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /*
         Sorts entries by points (descending) then earliest timestamp
         Displays top 3 in podium cards at top of screen
         Displays positions 4-10 in list below podium
         Updates UI immediately
     */
    private void displayLeaderboard(List<LeaderboardEntry> entries) {
        Collections.sort(entries, (a, b) -> {
            int pointsCompare = Integer.compare(b.points, a.points);
            if (pointsCompare != 0) return pointsCompare;
            return Long.compare(a.earliestTimestamp, b.earliestTimestamp);
        });

        if (entries.isEmpty()) return;

        int maxDisplay = Math.min(10, entries.size());
        List<LeaderboardEntry> top10 = entries.subList(0, maxDisplay);

        updateTopThreeCards(top10);

        rankingListContainer.removeAllViews();
        for (int i = 3; i < top10.size(); i++) {
            addLeaderboardItem(top10.get(i), i + 1);
        }
    }

    /*
         Updates three LinearLayout cards for 1st, 2nd, 3rd place
         Sets position text, point text, and student name
         Arranges as: 2nd (left), 1st (center), 3rd (right)
     */
    private void updateTopThreeCards(List<LeaderboardEntry> top10) {
        if (podiumContainer != null) {
            podiumContainer.setVisibility(View.VISIBLE);
        }

        if (podiumContainer != null && podiumContainer.getChildCount() >= 3) {
            if (top10.size() >= 2) {
                LinearLayout top2Card = (LinearLayout) podiumContainer.getChildAt(0);
                updateCardData(top2Card, top10.get(1), 2);
            }
          
            if (top10.size() >= 1) {
                LinearLayout top1Card = (LinearLayout) podiumContainer.getChildAt(1);
                updateCardData(top1Card, top10.get(0), 1);
            }
          
            if (top10.size() >= 3) {
                LinearLayout top3Card = (LinearLayout) podiumContainer.getChildAt(2);
                updateCardData(top3Card, top10.get(2), 3);
            }
        }
    }

    private void updateCardData(LinearLayout card, LeaderboardEntry entry, int position) {
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                if (i == 0) {
                    tv.setText(entry.fullName);
                } else if (i == 1) {
                    tv.setText(entry.points + " pts");
                } else if (i == 2) {
                    tv.setText("Top " + position);
                }
            }
        }
    }

    /*
         Creates LinearLayout for each ranking position
         Adds position number, student name, and points
         Applies styling and margins programmatically
         Adds to rankingListContainer
     */
    private void addLeaderboardItem(LeaderboardEntry entry, int position) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (60 * getResources().getDisplayMetrics().density)
        ));
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        itemLayout.setBackgroundResource(R.drawable.leaderboard_item_bg);
        itemLayout.setPadding(
                (int) (16 * getResources().getDisplayMetrics().density), 0,
                (int) (16 * getResources().getDisplayMetrics().density), 0
        );

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) itemLayout.getLayoutParams();
        layoutParams.setMargins(0, 0, 0, (int) (10 * getResources().getDisplayMetrics().density));
        itemLayout.setLayoutParams(layoutParams);

        TextView positionText = new TextView(this);
        positionText.setText(String.valueOf(position));
        positionText.setTextColor(0xFF000000);
        positionText.setTextSize(14);
        positionText.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams posParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        posParams.setMargins(0, 0, (int) (16 * getResources().getDisplayMetrics().density), 0);
        positionText.setLayoutParams(posParams);
        itemLayout.addView(positionText);

        TextView nameText = new TextView(this);
        nameText.setText(entry.fullName);
        nameText.setTextColor(0xFF000000);
        nameText.setTextSize(14);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        nameText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        itemLayout.addView(nameText);

        TextView pointsText = new TextView(this);
        pointsText.setText(entry.points + " pts");
        pointsText.setTextColor(0xFF000000);
        pointsText.setTextSize(14);
        pointsText.setTypeface(null, android.graphics.Typeface.BOLD);
        pointsText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        itemLayout.addView(pointsText);

        rankingListContainer.addView(itemLayout);
    }

    private void loadUserRanking(List<LeaderboardEntry> allEntries) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        int userPosition = -1;
        LeaderboardEntry userEntry = null;

        for (int i = 0; i < allEntries.size(); i++) {
            if (allEntries.get(i).userId.equals(currentUser.getUid())) {
                userPosition = i + 1;
                userEntry = allEntries.get(i);
                break;
            }
        }

        if (userEntry != null) {
            userRankingName.setText(userEntry.fullName);
            userRankingPoints.setText(userEntry.points + " pts");
            if (userRankingPosition != null) userRankingPosition.setText(String.valueOf(userPosition));
        } else {
            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName  = documentSnapshot.getString("lastName");
                            String fullName  = ((firstName != null ? firstName : "") +
                                    (lastName != null ? " " + lastName : "")).trim();
                            userRankingName.setText(fullName.isEmpty() ? "User" : fullName);
                        } else {
                            userRankingName.setText("User");
                        }
                    });
            userRankingPoints.setText("0 pts");
            if (userRankingPosition != null) userRankingPosition.setText("-");
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

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_home) {
                navigateToUserHome();
                return true;
            } else if (itemId == R.id.menu_profile) {
                startActivity(new Intent(Leaderboards.this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(Leaderboards.this, AttendanceStreak.class));
                return true;
            } else if (itemId == R.id.menu_leaderboards) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(Leaderboards.this, Leaderboards.class));
                return true;
            } else if (itemId == R.id.menu_attendance_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(Leaderboards.this, AttendanceHistoryActivity.class));
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(Leaderboards.this, ArchiveActivity.class));
                return true;
            } else if (itemId == R.id.menu_settings) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            } else if (itemId == R.id.menu_logout) {
                LogoutConfirmationDialog confirmDialog = new LogoutConfirmationDialog(this, this::logout,
                        () -> drawerLayout.closeDrawer(GravityCompat.START));
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
                                startActivity(new Intent(Leaderboards.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(Leaderboards.this, TeacherHome.class));
                            } else {
                                Toast.makeText(Leaderboards.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(Leaderboards.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Leaderboards.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(Leaderboards.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserInfoInDrawer() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView userNameTextView  = headerView.findViewById(R.id.drawer_user_name);
            TextView userEmailTextView = headerView.findViewById(R.id.drawer_user_email);

            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName  = documentSnapshot.getString("lastName");
                            String fullName  = "";
                            if (firstName != null && !firstName.isEmpty()) fullName = firstName;
                            if (lastName  != null && !lastName.isEmpty())  fullName += (fullName.isEmpty() ? "" : " ") + lastName;
                            userNameTextView.setText(fullName.isEmpty() ? "User" : fullName);
                        } else {
                            userNameTextView.setText("User");
                        }
                    })
                    .addOnFailureListener(e -> userNameTextView.setText("User"));

            userEmailTextView.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        }
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(Leaderboards.this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static class LeaderboardEntry {
        String userId;
        String fullName;
        int points;
        long earliestTimestamp;
    }

    private static class AttendanceRecord {
        String userId;
        long timestamp;
    }
}
