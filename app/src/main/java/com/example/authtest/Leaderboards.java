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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        loadLeaderboardData();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        userRankingName = findViewById(R.id.userRankingName);
        userRankingPoints = findViewById(R.id.userRankingPoints);
        rankingListContainer = findViewById(R.id.rankingListContainer);

        // Find the position TextView in the user ranking container
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
        backButton.setOnClickListener(view -> {
            finish();
        });
    }

    private void loadLeaderboardData() {
        // Get all attendance records
        db.collection("attendance")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Group attendance by date and class
                    Map<String, List<AttendanceRecord>> attendanceByClass = new HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getString("userId");
                        String status = doc.getString("status");
                        String date = doc.getString("date");
                        String classId = doc.getString("classId");
                        Long timestamp = doc.getLong("timestamp");

                        if (userId == null || !"present".equals(status) || date == null) continue;

                        // Create unique key for each class session (date + classId)
                        String classKey = date + "_" + (classId != null ? classId : "default");

                        AttendanceRecord record = new AttendanceRecord();
                        record.userId = userId;
                        record.timestamp = timestamp != null ? timestamp : Long.MAX_VALUE;

                        attendanceByClass.putIfAbsent(classKey, new ArrayList<>());
                        attendanceByClass.get(classKey).add(record);
                    }

                    // Calculate early bird points
                    Map<String, LeaderboardEntry> userPointsMap = new HashMap<>();

                    for (List<AttendanceRecord> records : attendanceByClass.values()) {
                        if (records.isEmpty()) continue;

                        // Sort by timestamp to find the earliest (first to mark)
                        Collections.sort(records, (a, b) -> Long.compare(a.timestamp, b.timestamp));

                        // Award points only to the first person (earliest timestamp)
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

                        // Track the earliest timestamp for tie-breaking
                        if (earlyBird.timestamp < entry.earliestTimestamp) {
                            entry.earliestTimestamp = earlyBird.timestamp;
                        }
                    }

                    // Fetch user names and display leaderboard
                    fetchUserNamesAndDisplay(new ArrayList<>(userPointsMap.values()));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading leaderboard: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchUserNamesAndDisplay(List<LeaderboardEntry> entries) {
        if (entries.isEmpty()) {
            Toast.makeText(this, "No leaderboard data available", Toast.LENGTH_SHORT).show();
            return;
        }

        final int[] loadedCount = {0};
        final int totalEntries = entries.size();

        // Fetch names for all users
        for (LeaderboardEntry entry : entries) {
            db.collection("users")
                    .document(entry.userId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String firstName = doc.getString("firstName");
                            String lastName = doc.getString("lastName");
                            entry.fullName = ((firstName != null ? firstName : "") +
                                    (lastName != null ? " " + lastName : "")).trim();

                            if (entry.fullName.isEmpty()) {
                                entry.fullName = "Unknown User";
                            }
                        } else {
                            entry.fullName = "Unknown User";
                        }

                        loadedCount[0]++;

                        // Check if all names are loaded
                        if (loadedCount[0] == totalEntries) {
                            displayLeaderboard(entries);
                            loadUserRanking(entries);
                        }
                    })
                    .addOnFailureListener(e -> {
                        entry.fullName = "Unknown User";
                        loadedCount[0]++;

                        if (loadedCount[0] == totalEntries) {
                            displayLeaderboard(entries);
                            loadUserRanking(entries);
                        }
                    });
        }
    }

    private void displayLeaderboard(List<LeaderboardEntry> entries) {
        // Sort by points descending, then by earliest timestamp ascending (for tie-breaking)
        Collections.sort(entries, (a, b) -> {
            int pointsCompare = Integer.compare(b.points, a.points);
            if (pointsCompare != 0) {
                return pointsCompare;
            }
            // If points are equal, sort by earliest timestamp (lower is better)
            return Long.compare(a.earliestTimestamp, b.earliestTimestamp);
        });

        if (entries.isEmpty()) {
            return;
        }

        // Limit to top 10 students only
        int maxDisplay = Math.min(10, entries.size());
        List<LeaderboardEntry> top10 = entries.subList(0, maxDisplay);

        // Update top 3 cards (they're already in the XML)
        updateTopThreeCards(top10);

        // Clear and populate the ranking list container (positions 4-10)
        rankingListContainer.removeAllViews();
        for (int i = 3; i < top10.size(); i++) {
            addLeaderboardItem(top10.get(i), i + 1);
        }
    }

    private void updateTopThreeCards(List<LeaderboardEntry> top10) {
        // Find the hardcoded cards in the layout
        LinearLayout podiumContainer = (LinearLayout) rankingListContainer.getParent();

        // Get the podium container (it's the LinearLayout before rankingListContainer)
        View podiumView = null;
        for (int i = 0; i < ((LinearLayout) podiumContainer).getChildCount(); i++) {
            View child = ((LinearLayout) podiumContainer).getChildAt(i);
            if (child instanceof LinearLayout) {
                View firstChild = ((LinearLayout) child).getChildAt(0);
                if (firstChild instanceof LinearLayout &&
                        ((LinearLayout) firstChild).getChildCount() == 3) {
                    podiumView = child;
                    break;
                }
            }
        }

        if (podiumView != null && podiumView instanceof LinearLayout) {
            LinearLayout podium = (LinearLayout) podiumView;

            // Update Top 2 (Left card - index 0)
            if (top10.size() >= 2 && podium.getChildCount() > 0) {
                LinearLayout top2Card = (LinearLayout) podium.getChildAt(0);
                updateCardData(top2Card, top10.get(1), 2);
            }

            // Update Top 1 (Center card - index 1)
            if (top10.size() >= 1 && podium.getChildCount() > 1) {
                LinearLayout top1Card = (LinearLayout) podium.getChildAt(1);
                updateCardData(top1Card, top10.get(0), 1);
            }

            // Update Top 3 (Right card - index 2)
            if (top10.size() >= 3 && podium.getChildCount() > 2) {
                LinearLayout top3Card = (LinearLayout) podium.getChildAt(2);
                updateCardData(top3Card, top10.get(2), 3);
            }
        }
    }

    private void updateCardData(LinearLayout card, LeaderboardEntry entry, int position) {
        // Find TextViews in the card
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                String hint = tv.getText().toString();

                // Update based on the existing text pattern
                if (hint.contains("Hitomo") || hint.contains("Baluyot") || hint.contains("Bloso")) {
                    tv.setText(entry.fullName);
                } else if (hint.contains("pts")) {
                    tv.setText(entry.points + " pts");
                } else if (hint.contains("Top")) {
                    tv.setText("Top " + position);
                }
            }
        }
    }

    private void addLeaderboardItem(LeaderboardEntry entry, int position) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (50 * getResources().getDisplayMetrics().density)
        ));
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        itemLayout.setBackgroundResource(R.drawable.leaderboard_item_bg);
        itemLayout.setPadding(
                (int) (16 * getResources().getDisplayMetrics().density),
                0,
                (int) (16 * getResources().getDisplayMetrics().density),
                0
        );

        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) itemLayout.getLayoutParams();
        layoutParams.setMargins(0, 0, 0, (int) (10 * getResources().getDisplayMetrics().density));
        itemLayout.setLayoutParams(layoutParams);

        // Position TextView
        TextView positionText = new TextView(this);
        positionText.setText(String.valueOf(position));
        positionText.setTextColor(0xFFFFFFFF);
        positionText.setTextSize(14);
        positionText.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams posParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        posParams.setMargins(0, 0, (int) (16 * getResources().getDisplayMetrics().density), 0);
        positionText.setLayoutParams(posParams);
        itemLayout.addView(positionText);

        // Name TextView
        TextView nameText = new TextView(this);
        nameText.setText(entry.fullName);
        nameText.setTextColor(0xFFFFFFFF);
        nameText.setTextSize(14);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        nameText.setLayoutParams(nameParams);
        itemLayout.addView(nameText);

        // Points TextView
        TextView pointsText = new TextView(this);
        pointsText.setText(entry.points + " pts");
        pointsText.setTextColor(0xFFFFFFFF);
        pointsText.setTextSize(14);
        pointsText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        itemLayout.addView(pointsText);

        rankingListContainer.addView(itemLayout);
    }

    private void loadUserRanking(List<LeaderboardEntry> allEntries) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        // Find user's position in the sorted list
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

            if (userRankingPosition != null) {
                userRankingPosition.setText(String.valueOf(userPosition));
            }
        } else {
            // User not in leaderboard
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
                    });
            userRankingPoints.setText("0 pts");

            if (userRankingPosition != null) {
                userRankingPosition.setText("-");
            }
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
        getOnBackPressedDispatcher()
                .addCallback(this, callback);
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_home) {
                navigateToUserHome();
                return true;
            } else if (itemId == R.id.menu_profile) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(Leaderboards.this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(Leaderboards.this, AttendanceStreak.class));
                return true;
            } else if (itemId == R.id.menu_leaderboards) {
                drawerLayout.closeDrawer(GravityCompat.START);
                // Already on Leaderboards, just close drawer
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
                startActivity(new Intent(Leaderboards.this, SettingsActivity.class));
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