package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class AddStudentsForm extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private AppCompatButton backButton;
    private AppCompatButton addStudentButton;
    private EditText studentEmailInput;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String classId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_students_form);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        classId = getIntent().getStringExtra("CLASS_ID");
        
        if (classId == null || classId.isEmpty()) {
            Toast.makeText(this, "Error: Class ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        ImageView hamburgerIcon = findViewById(R.id.hamburger_icon);
        hamburgerIcon.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        setupNavigationDrawer();
        setupBackPressHandler();
        loadUserInfoInDrawer();

        backButton = findViewById(R.id.backButton);
        addStudentButton = findViewById(R.id.addStudentToClassButton);
        studentEmailInput = findViewById(R.id.studentEmailInput);

        backButton.setOnClickListener(view -> {
            finish();
        });

        addStudentButton.setOnClickListener(view -> {
            addStudentToClass();
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
                startActivity(new Intent(this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Streak feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Archive feature coming soon", Toast.LENGTH_SHORT).show();
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
                                startActivity(new Intent(AddStudentsForm.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(AddStudentsForm.this, TeacherHome.class));
                            } else {
                                Toast.makeText(AddStudentsForm.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(AddStudentsForm.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AddStudentsForm.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(AddStudentsForm.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(AddStudentsForm.this, SignIn.class);
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

    private void addStudentToClass() {
        String studentEmail = studentEmailInput.getText().toString().trim().toLowerCase();

        if (studentEmail.isEmpty()) {
            studentEmailInput.setError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(studentEmail).matches()) {
            studentEmailInput.setError("Please enter a valid email");
            return;
        }

        if (!studentEmail.contains("@students.")) {
            studentEmailInput.setError("Must be a student email (@students.)");
            return;
        }

        addStudentButton.setEnabled(false);
        addStudentButton.setText("Adding Student...");

        String teacherId = mAuth.getCurrentUser().getUid();
        checkIfStudentExists(studentEmail, teacherId);
    }

    private void checkIfStudentExists(String studentEmail, String teacherId) {
        db.collection("allClasses")
                .document(classId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        resetButton();
                        Toast.makeText(this, "Class not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> allowedEmails =
                            (List<String>) document.get("allowedStudentEmails");

                    if (allowedEmails != null && allowedEmails.contains(studentEmail)) {
                        studentEmailInput.setError("Student already added");
                        resetButton();
                        return;
                    }

                    addStudentToClassFirebase(studentEmail, teacherId);
                })
                .addOnFailureListener(e -> {
                    resetButton();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addStudentToClassFirebase(String studentEmail, String teacherId) {
        addStudentButton.setText("Adding Student...");

        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update(
                        "allowedStudentEmails", FieldValue.arrayUnion(studentEmail),
                        "students", FieldValue.increment(1)
                )
                .addOnSuccessListener(unused -> {
                    db.collection("allClasses")
                            .document(classId)
                            .update(
                                    "allowedStudentEmails", FieldValue.arrayUnion(studentEmail),
                                    "students", FieldValue.increment(1)
                            )
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(this, "Student added successfully!", Toast.LENGTH_SHORT).show();
                                studentEmailInput.setText("");
                                resetButton();
                                setResult(RESULT_OK);
                            })
                            .addOnFailureListener(e -> {
                                resetButton();
                                Toast.makeText(this, "Failed to update allClasses: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    resetButton();
                    Toast.makeText(this, "Failed to add student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void resetButton() {
        addStudentButton.setEnabled(true);
        addStudentButton.setText("Add Student");
    }
}