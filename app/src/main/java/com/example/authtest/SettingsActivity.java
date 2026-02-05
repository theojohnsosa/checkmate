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

        findViewById(R.id.hamburger_icon).setOnClickListener(view -> {
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
        checkUserTypeAndConfigureMenu();
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

    private void toggleDoNotDisturb(boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                showDNDPermissionDialog(enable);
                return;
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (enable) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                    Toast.makeText(this, "Do Not Disturb enabled", Toast.LENGTH_SHORT).show();
                } else {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                    Toast.makeText(this, "Do Not Disturb disabled", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error changing Do Not Disturb: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            doNotDisturbToggle.setOnCheckedChangeListener(null);
            doNotDisturbToggle.setChecked(!enable);
            doNotDisturbToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                toggleDoNotDisturb(isChecked);
            });
        }
    }

    private void showDNDPermissionDialog(boolean shouldEnable) {
        doNotDisturbToggle.setOnCheckedChangeListener(null);
        doNotDisturbToggle.setChecked(!shouldEnable);
        doNotDisturbToggle.setOnCheckedChangeListener((buttonView, isChecked) -> toggleDoNotDisturb(isChecked));
        Toast.makeText(this, "Please grant Do Not Disturb permission in Settings", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivity(intent);
    }

    private void loadDNDState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            doNotDisturbToggle.setOnCheckedChangeListener(null);

            int interruptionFilter = notificationManager.getCurrentInterruptionFilter();
            boolean isDNDEnabled = interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                    interruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
                    interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS;

            doNotDisturbToggle.setChecked(isDNDEnabled);
            doNotDisturbToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                toggleDoNotDisturb(isChecked);
            });
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
        startActivity(new Intent(SettingsActivity.this, ShareFeedbackActivity.class));
    }

    private void openTermsOfServices() {
        startActivity(new Intent(SettingsActivity.this, TermsOfServices.class));
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
                () -> {}
        );
        confirmDialog.show();
    }
}