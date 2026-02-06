package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class PrivacyPolicy extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        findViewById(R.id.hamburger_icon).setOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        findViewById(R.id.backButton).setOnClickListener(view -> {
            finish();
        });

        findViewById(R.id.agreeButton).setOnClickListener(view -> {
            Toast.makeText(this, "You have agreed to the Privacy Policy", Toast.LENGTH_SHORT).show();
        });

        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });
    }

    private void setupNavigationDrawer() {
        navigationView.getMenu().findItem(R.id.menu_streak).setVisible(false);

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_home) {
                navigateToUserHome();
            } else if (itemId == R.id.menu_profile) {
                startActivity(new Intent(PrivacyPolicy.this, ProfilePage.class));
            } else if (itemId == R.id.menu_streak) {
                startActivity(new Intent(PrivacyPolicy.this, AttendanceStreak.class));
            } else if (itemId == R.id.menu_attendance_history) {
                startActivity(new Intent(PrivacyPolicy.this, AttendanceHistoryActivity.class));
            } else if (itemId == R.id.menu_archive) {
                startActivity(new Intent(PrivacyPolicy.this, ArchiveActivity.class));
            } else if (itemId == R.id.menu_settings) {
                startActivity(new Intent(PrivacyPolicy.this, SettingsActivity.class));
            } else if (itemId == R.id.menu_logout) {
                showLogoutConfirmation();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void showLogoutConfirmation() {
        LogoutConfirmationDialog confirmDialog = new LogoutConfirmationDialog(this, this::logout,
                () -> {

                });
        confirmDialog.show();
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(PrivacyPolicy.this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToUserHome() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String userType = documentSnapshot.getString("userType");
                if ("Student".equalsIgnoreCase(userType)) {
                    startActivity(new Intent(PrivacyPolicy.this, StudentHome.class));
                } else if ("Teacher".equalsIgnoreCase(userType)) {
                    startActivity(new Intent(PrivacyPolicy.this, TeacherHome.class));
                }
                finish();
            }
        });
    }

    private void loadUserInfoInDrawer() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            View headerView = navigationView.getHeaderView(0);
            TextView userNameTextView = headerView.findViewById(R.id.drawer_user_name);
            TextView userEmailTextView = headerView.findViewById(R.id.drawer_user_email);

            db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String userType = documentSnapshot.getString("userType");
                    if ("Student".equalsIgnoreCase(userType)) {
                        navigationView.getMenu().findItem(R.id.menu_streak).setVisible(true);
                    }

                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                    userNameTextView.setText(fullName.trim().isEmpty() ? "User" : fullName.trim());
                }
            });

            userEmailTextView.setText(currentUser.getEmail());
        }
    }
}