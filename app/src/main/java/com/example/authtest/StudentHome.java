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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.authtest.databinding.ActivityStudentHomeBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class StudentHome extends AppCompatActivity {

    private ActivityStudentHomeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ClassAdapter classAdapter;
    private List<ClassModel> classList = new ArrayList<>();
    private boolean isLoadingClasses = false;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStudentHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize DrawerLayout and NavigationView using findViewById
        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        // Hamburger icon click listener
        binding.hamburgerIcon.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Setup navigation drawer menu items
        setupNavigationDrawer();

        // Load user info in drawer header
        loadUserInfoInDrawer();

        // Setup back press handler
        setupBackPressHandler();

        setupRecyclerView();
        loadClasses();

        binding.joinClassButton.setOnClickListener(v -> {
            JoinClassDialog dialog = new JoinClassDialog(this, this::loadClasses);
            dialog.show();
        });

        binding.ctaButton.setOnClickListener(v -> {
            JoinClassDialog dialog = new JoinClassDialog(this, this::loadClasses);
            dialog.show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isLoadingClasses) {
            loadClasses();
        }
    }

    private void setupRecyclerView() {
        classAdapter = new ClassAdapter(classModel -> {
            Intent intent = new Intent(StudentHome.this, ClassInformation.class);
            intent.putExtra("CLASS_MODEL", classModel);
            startActivity(intent);
        });

        binding.classesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.classesRecyclerView.setAdapter(classAdapter);

        classAdapter.setClasses(classList);
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

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menu_home) {
                navigateToUserHome();
                return true;
            } else if (itemId == R.id.menu_profile) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Profile feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_settings) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_logout) {
                logout();
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
                                startActivity(new Intent(StudentHome.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(StudentHome.this, TeacherHome.class));
                            } else {
                                Toast.makeText(StudentHome.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(StudentHome.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(StudentHome.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(StudentHome.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(StudentHome.this, SignIn.class);
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

            // Fetch user details from Firestore
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

    private void loadClasses() {
        if (isLoadingClasses) {
            return;
        }

        isLoadingClasses = true;
        String studentId = mAuth.getCurrentUser().getUid();

        classList.clear();
        classAdapter.notifyDataSetChanged();

        db.collection("users")
                .document(studentId)
                .collection("enrolledClasses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        isLoadingClasses = false;
                        updateUI();
                        return;
                    }

                    final int totalClasses = querySnapshot.size();
                    final int[] loadedClasses = {0};

                    for (var doc : querySnapshot) {
                        String classId = doc.getString("classId");

                        if (classId != null) {
                            db.collection("allClasses")
                                    .document(classId)
                                    .get()
                                    .addOnSuccessListener(classDoc -> {
                                        if (classDoc.exists()) {
                                            ClassModel model = classDoc.toObject(ClassModel.class);
                                            if (model != null) {
                                                model.setId(classDoc.getId());
                                                classList.add(model);
                                            }
                                        }

                                        loadedClasses[0]++;
                                        if (loadedClasses[0] == totalClasses) {
                                            classAdapter.notifyDataSetChanged();
                                            updateUI();
                                            isLoadingClasses = false;
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        loadedClasses[0]++;
                                        if (loadedClasses[0] == totalClasses) {
                                            classAdapter.notifyDataSetChanged();
                                            updateUI();
                                            isLoadingClasses = false;
                                        }
                                    });
                        } else {
                            loadedClasses[0]++;
                            if (loadedClasses[0] == totalClasses) {
                                classAdapter.notifyDataSetChanged();
                                updateUI();
                                isLoadingClasses = false;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    isLoadingClasses = false;
                    updateUI();
                });
    }

    private void updateUI() {
        boolean isEmpty = classList.isEmpty();
        binding.emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.classesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}