package com.example.authtest;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import android.widget.LinearLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private Switch appearanceToggle;
    private Switch notificationsToggle;
    private Switch doNotDisturbToggle;
    private LinearLayout appIconButton;
    private LinearLayout shareFeedbackButton;
    private LinearLayout termsOfServicesButton;
    private LinearLayout privacyPolicyButton;
    private LinearLayout faqsButton;
    private AppCompatButton logoutButton;
    private AppCompatButton backButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        findViewById(R.id.hamburger_icon).setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();
        initializeViews();
        setupToggleListeners();
        setupButtonListeners();
        setupBackButton();
        loadDNDState();
    }

    private void initializeViews() {
        appearanceToggle = findViewById(R.id.appearanceToggle);
        notificationsToggle = findViewById(R.id.notificationsToggle);
        doNotDisturbToggle = findViewById(R.id.doNotDisturbToggle);
        appIconButton = findViewById(R.id.appIconButton);
        shareFeedbackButton = findViewById(R.id.shareFeedbackButton);
        termsOfServicesButton = findViewById(R.id.termsOfServicesButton);
        privacyPolicyButton = findViewById(R.id.privacyPolicyButton);
        faqsButton = findViewById(R.id.faqsButton);
        logoutButton = findViewById(R.id.logoutButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupToggleListeners() {
        appearanceToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Appearance feature coming soon", Toast.LENGTH_SHORT).show();
        });

        notificationsToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, "Notifications feature coming soon", Toast.LENGTH_SHORT).show();
        });

        doNotDisturbToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleDoNotDisturb(isChecked);
        });
    }

    /**
     * Toggles the Do Not Disturb mode on the device
     * @param enable true to enable DND, false to disable
     */
    private void toggleDoNotDisturb(boolean enable) {
        // Check if app has permission to access DND settings (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                // Permission not granted, open settings
                showDNDPermissionDialog(enable);
                return;
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (enable) {
                    // Enable Do Not Disturb
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                    Toast.makeText(this, "Do Not Disturb enabled", Toast.LENGTH_SHORT).show();
                } else {
                    // Disable Do Not Disturb
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                    Toast.makeText(this, "Do Not Disturb disabled", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error changing Do Not Disturb: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // Revert the toggle
            doNotDisturbToggle.setOnCheckedChangeListener(null);
            doNotDisturbToggle.setChecked(!enable);
            doNotDisturbToggle.setOnCheckedChangeListener((buttonView, isChecked) -> toggleDoNotDisturb(isChecked));
        }
    }

    /**
     * Shows a dialog to guide user to grant DND permission
     */
    private void showDNDPermissionDialog(boolean shouldEnable) {
        // Revert toggle
        doNotDisturbToggle.setOnCheckedChangeListener(null);
        doNotDisturbToggle.setChecked(!shouldEnable);
        doNotDisturbToggle.setOnCheckedChangeListener((buttonView, isChecked) -> toggleDoNotDisturb(isChecked));

        Toast.makeText(this, "Please grant Do Not Disturb permission in Settings", Toast.LENGTH_LONG).show();

        // Open notification settings
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivity(intent);
    }

    /**
     * Loads the current DND state from the device and updates the toggle
     */
    private void loadDNDState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            doNotDisturbToggle.setOnCheckedChangeListener(null); // Disable listener to prevent triggering event

            int interruptionFilter = notificationManager.getCurrentInterruptionFilter();
            boolean isDNDEnabled = interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                    interruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
                    interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS;

            doNotDisturbToggle.setChecked(isDNDEnabled);
            doNotDisturbToggle.setOnCheckedChangeListener((buttonView, isChecked) -> toggleDoNotDisturb(isChecked));
        }
    }

    private void setupButtonListeners() {
        appIconButton.setOnClickListener(view -> {
            openAppIcon();
        });

        shareFeedbackButton.setOnClickListener(view -> {
            shareFeeback();
        });

        termsOfServicesButton.setOnClickListener(view -> {
            openTermsOfServices();
        });

        privacyPolicyButton.setOnClickListener(view -> {
            openPrivacyPolicy();
        });

        faqsButton.setOnClickListener(view -> {
            openFAQs();
        });

        logoutButton.setOnClickListener(view -> {
            showLogoutConfirmation();
        });
    }

    private void setupBackButton() {
        backButton.setOnClickListener(view -> {
            finish();
        });
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
                startActivity(new Intent(SettingsActivity.this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(SettingsActivity.this, AttendanceStreak.class));
                return true;
            } else if (itemId == R.id.menu_attendance_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(SettingsActivity.this, AttendanceHistoryActivity.class));
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(SettingsActivity.this, ArchiveActivity.class));
                return true;
            } else if (itemId == R.id.menu_settings) {
                drawerLayout.closeDrawer(GravityCompat.START);
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
                                startActivity(new Intent(SettingsActivity.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(SettingsActivity.this, TeacherHome.class));
                            } else {
                                Toast.makeText(SettingsActivity.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SettingsActivity.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(SettingsActivity.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(SettingsActivity.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void openAppIcon() {
        AppIconSelectionDialog iconDialog = new AppIconSelectionDialog(
                this,
                () -> {
                },
                () -> {
                }
        );
        iconDialog.show();
    }

    private void shareFeeback() {
        Toast.makeText(this, "Share Feedback feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void openTermsOfServices() {
        Toast.makeText(this, "Terms of Services feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void openPrivacyPolicy() {
        Toast.makeText(this, "Privacy Policy feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void openFAQs() {
        startActivity(new Intent(SettingsActivity.this, Faqs.class));
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(SettingsActivity.this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLogoutConfirmation() {
        LogoutConfirmationDialog confirmDialog = new LogoutConfirmationDialog(
                this,
                this::logout,
                () -> {
                }
        );
        confirmDialog.show();
    }
}