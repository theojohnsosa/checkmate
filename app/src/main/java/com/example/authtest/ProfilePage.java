package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

public class ProfilePage extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView hamburgerIcon;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AppCompatButton backButton;
    private TextView profileUserName;
    private TextView profileEmail;
    private TextView profileSchoolNumber;
    private TextView profileDepartment;
    private TextView profileUserType;
    private TextView profileYear;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupBackButton();
        setupBackPressHandler();
        setupListeners();
        setupNavigationDrawer();
        loadUserInfoInDrawer();
        loadUserProfileInfo();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.main);
        hamburgerIcon = findViewById(R.id.hamburger_icon);
        navigationView = findViewById(R.id.navigation_view_profile);
        backButton = findViewById(R.id.backButton);
        profileUserName = findViewById(R.id.profile_user_name);
        profileEmail = findViewById(R.id.profile_email);
        profileSchoolNumber = findViewById(R.id.profile_school_number);
        profileDepartment = findViewById(R.id.profile_department);
        profileUserType = findViewById(R.id.profile_user_type);
        profileYear = findViewById(R.id.profile_year);
        logoutButton = findViewById(R.id.logoutButton);
    }
    private void setupBackButton() {
        backButton.setOnClickListener(v -> finish());
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
    private void setupListeners() {
        hamburgerIcon.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        logoutButton.setOnClickListener(v -> {
            LogoutConfirmationDialog confirmDialog = new LogoutConfirmationDialog(
                    this,
                    this::logout,
                    () -> {}
            );
            confirmDialog.show();
        });
    }
    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_home) {
                navigateToUserHome();
                return true;
            } else if (itemId == R.id.menu_profile) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Streak feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_attendance_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(ProfilePage.this, AttendanceHistoryActivity.class));
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(ProfilePage.this, ArchiveActivity.class));
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
                                startActivity(new Intent(ProfilePage.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(ProfilePage.this, TeacherHome.class));
                            } else {
                                Toast.makeText(ProfilePage.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ProfilePage.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ProfilePage.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(ProfilePage.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(ProfilePage.this, SignIn.class);
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
    private void loadUserProfileInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            profileEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "N/A");

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

                            profileUserName.setText(fullName.isEmpty() ? "User" : fullName);

                            String schoolNumber = documentSnapshot.getString("schoolNumber");
                            String department = documentSnapshot.getString("department");
                            String userType = documentSnapshot.getString("userType");
                            String year = documentSnapshot.getString("yearLevel");

                            profileSchoolNumber.setText(schoolNumber != null && !schoolNumber.isEmpty() ? schoolNumber : "N/A");
                            profileDepartment.setText(department != null && !department.isEmpty() ? department : "N/A");
                            profileUserType.setText(userType != null && !userType.isEmpty() ? userType : "N/A");
                            profileYear.setText(year != null && !year.isEmpty() ? year : "N/A");
                        } else {
                            profileUserName.setText("User");
                            profileSchoolNumber.setText("N/A");
                            profileDepartment.setText("N/A");
                            profileUserType.setText("N/A");
                            profileYear.setText("N/A");
                        }
                    })
                    .addOnFailureListener(e -> {
                        profileUserName.setText("User");
                        profileSchoolNumber.setText("N/A");
                        profileDepartment.setText("N/A");
                        profileUserType.setText("N/A");
                        profileYear.setText("N/A");
                    });
        }
    }
}