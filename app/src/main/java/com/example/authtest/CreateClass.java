package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class CreateClass extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private EditText classNameInput, subjectCodeInput, roomInput;
    private ToggleButton monToggle, tueToggle, wedToggle, thuToggle, friToggle, satToggle;
    private AutoCompleteTextView startTimeInput, endTimeInput;
    private AppCompatButton createClassButton, backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        ImageView hamburgerIcon = findViewById(R.id.hamburger_icon);

        hamburgerIcon.setOnClickListener(view ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        setupNavigationDrawer();
        setupBackPressHandler();
        loadUserInfoInDrawer();
        bindViews();
        setupTimeDropdowns();
        checkUserTypeAndConfigureMenu();

        backButton.setOnClickListener(view -> {
            finish();
        });

        createClassButton.setOnClickListener(view -> {
            if (validateInputs()) {
                createClassButton.setEnabled(false);
                createClassButton.setText("Checking...");
                createClass(createClassModel());
            }
        });
    }

    private void bindViews() {
        classNameInput = findViewById(R.id.classNameInput);
        subjectCodeInput = findViewById(R.id.subjectCodeInput);
        roomInput = findViewById(R.id.roomInput);

        monToggle = findViewById(R.id.monToggle);
        tueToggle = findViewById(R.id.tueToggle);
        wedToggle = findViewById(R.id.wedToggle);
        thuToggle = findViewById(R.id.thuToggle);
        friToggle = findViewById(R.id.friToggle);
        satToggle = findViewById(R.id.satToggle);

        startTimeInput = findViewById(R.id.startTimeInput);
        endTimeInput = findViewById(R.id.endTimeInput);

        createClassButton = findViewById(R.id.createClassButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_home) {
                navigateToUserHome();
                return true;
            } else if (itemId == R.id.menu_profile) {
                startActivity(new Intent(CreateClass.this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(CreateClass.this, AttendanceStreak.class));
                return true;
            } else if (itemId == R.id.menu_leaderboards) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(CreateClass.this, Leaderboards.class));
                return true;
            } else if (itemId == R.id.menu_attendance_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(CreateClass.this, AttendanceHistoryActivity.class));
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(CreateClass.this, ArchiveActivity.class));
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
                                startActivity(new Intent(CreateClass.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(CreateClass.this, TeacherHome.class));
                            } else {
                                Toast.makeText(CreateClass.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(CreateClass.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(CreateClass.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(CreateClass.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(CreateClass.this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            drawerLayout.closeDrawer(GravityCompat.START);
                        } else {
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
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

    private ClassModel createClassModel() {
        FirebaseUser user = mAuth.getCurrentUser();
        String teacherId = user != null ? user.getUid() : null;
        return new ClassModel(
                classNameInput.getText().toString().trim(),
                generateClassCode(),
                subjectCodeInput.getText().toString().trim(),
                getSelectedDays(),
                startTimeInput.getText().toString().trim(),
                endTimeInput.getText().toString().trim(),
                roomInput.getText().toString().trim(),
                resolveTeacherName(user),
                0,
                teacherId
        );
    }

    private String resolveTeacherName(FirebaseUser user) {
        if (user == null) {
            return "Instructor";
        }

        String displayName = user.getDisplayName();

        if (displayName == null || displayName.trim().isEmpty()) {
            return "Instructor";
        }

        String[] parts = displayName.trim().split("\\s+");

        if (parts.length >= 2) {
            return parts[0] + " " + parts[parts.length - 1];
        }

        return parts[0];
    }


    private void createClass(ClassModel model) {
        String teacherId = mAuth.getCurrentUser().getUid();

        model.setCreatedAt(System.currentTimeMillis());

        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .add(model)
                .addOnSuccessListener(ref -> {
                    String classId = ref.getId();
                    model.setId(classId);
                    db.collection("allClasses").document(classId).set(model);
                    Toast.makeText(this, "Class created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create class", Toast.LENGTH_LONG).show();
                    createClassButton.setEnabled(true);
                    createClassButton.setText("Create New Class");
                });
    }

    private void setupTimeDropdowns() {
        List<String> times = new ArrayList<>();
        for (int i = 7; i <= 21; i++) {
            int hour = i % 12 == 0 ? 12 : i % 12;
            String period = i < 12 ? "AM" : "PM";
            times.add(hour + ":00 " + period);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, times);

        startTimeInput.setAdapter(adapter);
        endTimeInput.setAdapter(adapter);

        startTimeInput.setOnClickListener(view -> {
            startTimeInput.showDropDown();
        });

        endTimeInput.setOnClickListener(view -> {
            endTimeInput.showDropDown();
        });
    }

    private String getSelectedDays() {
        List<String> days = new ArrayList<>();
        if (monToggle.isChecked()) days.add("Mon");
        if (tueToggle.isChecked()) days.add("Tue");
        if (wedToggle.isChecked()) days.add("Wed");
        if (thuToggle.isChecked()) days.add("Thu");
        if (friToggle.isChecked()) days.add("Fri");
        if (satToggle.isChecked()) days.add("Sat");
        return String.join("/", days);
    }

    private String generateClassCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }

    private boolean validateInputs() {
        if (classNameInput.getText().toString().trim().isEmpty()) {
            classNameInput.setError("Required");
            return false;
        }

        if (subjectCodeInput.getText().toString().trim().isEmpty()) {
            subjectCodeInput.setError("Required");
            return false;
        }

        if (getSelectedDays().isEmpty()) {
            Toast.makeText(this, "Select at least one day", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (startTimeInput.getText().toString().trim().isEmpty()) {
            startTimeInput.setError("Required");
            return false;
        }

        if (endTimeInput.getText().toString().trim().isEmpty()) {
            endTimeInput.setError("Required");
            return false;
        }

        if (roomInput.getText().toString().trim().isEmpty()) {
            roomInput.setError("Required");
            return false;
        }

        return true;
    }
}