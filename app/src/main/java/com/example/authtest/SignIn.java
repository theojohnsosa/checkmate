package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.google.firebase.auth.FirebaseAuth;

// Login screen for existing users
public class SignIn extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText schoolEmailInput;
    private EditText passwordInput;
    private AppCompatButton signInButton;
    private AppCompatButton hasNoAccountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

        schoolEmailInput = findViewById(R.id.schoolEmailInput);
        passwordInput = findViewById(R.id.passwordInput);
        signInButton = findViewById(R.id.signInButton);
        hasNoAccountButton = findViewById(R.id.hasNoAccountButton);

        signInButton.setOnClickListener(view -> {
            signInUser();
        });

        hasNoAccountButton.setOnClickListener(view -> {
            Intent intent = new Intent(SignIn.this, SignUp.class);
            startActivity(intent);
            finish();
        });
    }

    /*
        Gets email and password from EditText inputs
        Calls validateInputs() to check both fields
        Uses FirebaseAuth.signInWithEmailAndPassword() for authentication
        On success, checks email domain to route to StudentHome or TeacherHome
        On failure, shows error message from Firebase
     */
    private void signInUser() {
        String schoolEmail = schoolEmailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!validateInputs(schoolEmail, password)) {
            return;
        }

        signInButton.setEnabled(false);
        signInButton.setText("Signing in...");

        mAuth.signInWithEmailAndPassword(schoolEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        String email = mAuth.getCurrentUser().getEmail();

                        Intent intent;

                        if (email != null && email.contains("@students.")) {
                            intent = new Intent(SignIn.this, StudentHome.class);
                        } else {
                            intent = new Intent(SignIn.this, TeacherHome.class);
                        }

                        Toast.makeText(SignIn.this, "Sign in successful!", Toast.LENGTH_SHORT).show();

                        startActivity(intent);
                        finish();

                    } else {
                        signInButton.setEnabled(true);
                        signInButton.setText("Login");

                        String errorMessage = "Sign in failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        Toast.makeText(SignIn.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });

    }

    /*
         Checks email not empty
         Checks email format with Patterns.EMAIL_ADDRESS regex
         Checks password not empty
         Sets error on field and requests focus for first failing field
     */
    private boolean validateInputs(String schoolEmail, String password) {
        if (schoolEmail.isEmpty()) {
            schoolEmailInput.setError("Email is required");
            schoolEmailInput.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(schoolEmail).matches()) {
            schoolEmailInput.setError("Please enter a valid email");
            schoolEmailInput.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }

        return true;
    }
}