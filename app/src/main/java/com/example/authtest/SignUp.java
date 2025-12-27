package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText firstNameInput;
    private EditText lastNameInput;
    private EditText schoolEmailInput;
    private EditText passwordInput;
    private AutoCompleteTextView userTypeInput;
    private AutoCompleteTextView departmentInput;
    private EditText schoolNumberInput;
    private AutoCompleteTextView yearLevelInput;
    private AppCompatButton signUpButton;
    private AppCompatButton hasAccountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        schoolEmailInput = findViewById(R.id.schoolEmailInput);
        passwordInput = findViewById(R.id.passwordInput);
        userTypeInput = findViewById(R.id.userTypeInput);
        departmentInput = findViewById(R.id.departmentInput);
        schoolNumberInput = findViewById(R.id.schoolNumberInput);
        yearLevelInput = findViewById(R.id.yearLevelInput);
        signUpButton = findViewById(R.id.signUpButton);
        hasAccountButton = findViewById(R.id.hasAccountButton);

        String[] userTypes = {"Student", "Teacher"};
        ArrayAdapter<String> userTypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                userTypes
        );
        userTypeInput.setAdapter(userTypeAdapter);

        userTypeInput.setOnClickListener(v -> {
            userTypeInput.showDropDown();
        });
        userTypeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) userTypeInput.showDropDown();
        });
        userTypeInput.setKeyListener(null);

        userTypeInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedType = (String) parent.getItemAtPosition(position);

            if ("Teacher".equals(selectedType)) {
                schoolEmailInput.setHint("Teacher email");
                schoolNumberInput.setHint("Teacher number");
                schoolEmailInput.setText("");
                schoolNumberInput.setText("");
            } else {
                schoolEmailInput.setHint("Student email");
                schoolNumberInput.setHint("Student number");
                schoolEmailInput.setText("");
                schoolNumberInput.setText("");
            }
        });

        String[] departments = {
                "Bachelor of Science in Accountancy",
                "Bachelor of Science in Business Administration",
                "Bachelor of Science in Tourism Management",
                "Bachelor of Science in Architecture",
                "Bachelor of Science in Information Technology",
                "Bachelor of Science in Psychology"
        };
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                departments
        );
        departmentInput.setAdapter(departmentAdapter);

        departmentInput.setOnClickListener(v -> {
            departmentInput.showDropDown();
        });
        departmentInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                departmentInput.showDropDown();
            }
        });
        departmentInput.setKeyListener(null);

        String[] yearLevels = {"1st Year", "2nd Year", "3rd Year", "4th Year"};
        ArrayAdapter<String> yearLevelAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                yearLevels
        );
        yearLevelInput.setAdapter(yearLevelAdapter);

        yearLevelInput.setOnClickListener(v -> {
            yearLevelInput.showDropDown();
        });
        yearLevelInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                yearLevelInput.showDropDown();
            }
        });
        yearLevelInput.setKeyListener(null);

        signUpButton.setOnClickListener(v -> {
           createAccount();
        });

        hasAccountButton.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, SignIn.class);
            startActivity(intent);
            finish();
        });
    }

    private void createAccount() {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String schoolEmail = schoolEmailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String userType = userTypeInput.getText().toString().trim();
        String department = departmentInput.getText().toString().trim();
        String schoolNumber = schoolNumberInput.getText().toString().trim();
        String yearLevel = yearLevelInput.getText().toString().trim();

        if (!validateInputs(firstName,
                lastName,
                schoolEmail,
                password,
                userType,
                department,
                schoolNumber,
                yearLevel)) {
            return;
        }

        signUpButton.setEnabled(false);
        signUpButton.setText("Creating Account...");

        mAuth.createUserWithEmailAndPassword(schoolEmail, password)
                .addOnCompleteListener(this, task -> {
                   if (task.isSuccessful()) {
                       FirebaseUser firebaseUser = mAuth.getCurrentUser();
                       if (firebaseUser != null) {
                           saveUserToFirestore(firebaseUser.getUid(),
                                   firstName,
                                   lastName,
                                   schoolEmail,
                                   userType,
                                   department,
                                   schoolNumber,
                                   yearLevel);
                       }
                   } else {
                       signUpButton.setEnabled(true);
                       signUpButton.setText("Create Account");

                       String errorMessage = "Sign up failed";
                       if (task.getException() != null) {
                           errorMessage = task.getException().getMessage();
                       }

                       Toast.makeText(SignUp.this, errorMessage, Toast.LENGTH_LONG).show();
                   }
                });
    }

    private boolean validateInputs(
            String firstName,
            String lastName,
            String schoolEmail,
            String password,
            String userType,
            String department,
            String schoolNumber,
            String yearLevel) {

        if (firstName.isEmpty()) {
            firstNameInput.setError("First name is required");
            firstNameInput.requestFocus();
            return false;
        }

        if (lastName.isEmpty()) {
            lastNameInput.setError("Last name is required");
            lastNameInput.requestFocus();
            return false;
        }

        if (schoolEmail.isEmpty()) {
            schoolEmailInput.setError("Email is required");
            schoolEmailInput.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(schoolEmail).matches()) {
            schoolEmailInput.setError("Please enter valid email");
            schoolEmailInput.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return false;
        }

        if (userType.isEmpty()) {
            userTypeInput.setError("User type is required");
            userTypeInput.requestFocus();
            Toast.makeText(this, "Please select a user type", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (department.isEmpty()) {
            departmentInput.setError("Department is required");
            departmentInput.requestFocus();
            Toast.makeText(this, "Please select a department", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (schoolNumber.isEmpty()) {
            schoolNumberInput.setError("School number is required");
            schoolNumberInput.requestFocus();
            return false;
        }

        if (yearLevel.isEmpty()) {
            yearLevelInput.setError("Year level is required");
            yearLevelInput.requestFocus();
            Toast.makeText(this, "Please select a year level", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveUserToFirestore(
            String userId,
            String firstName,
            String lastName,
            String schoolEmail,
            String userType,
            String department,
            String schoolNumber,
            String yearLevel
            ) {
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("schoolEmail", schoolEmail);
        user.put("userType", userType);
        user.put("department", department);
        user.put("schoolNumber", schoolNumber);
        user.put("yearLevel", yearLevel);
        user.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId)
                .set(user)
                .addOnCompleteListener(aVoid -> {
                   Toast.makeText(SignUp.this,
                           "Account created successfully!",
                           Toast.LENGTH_SHORT).show();
                   Intent intent = new Intent(SignUp.this, TeacherHome.class);
                   startActivity(intent);
                   finish();
                })
                .addOnFailureListener(e -> {
                    signUpButton.setEnabled(true);
                    signUpButton.setText("Create Account");
                    Toast.makeText(SignUp.this,
                            "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) {
                        currentUser.delete();
                    }
                });
    }

}