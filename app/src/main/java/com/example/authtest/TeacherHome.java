package com.example.authtest;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.authtest.databinding.ActivityTeacherHomeBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeacherHome extends AppCompatActivity implements ClassAdapter.OnClassClickListener {

    private static final String TAG = "TeacherHome";
    private ActivityTeacherHomeBinding binding;
    private ClassAdapter classAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private List<ClassModel> classList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTeacherHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        binding.classesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        classAdapter = new ClassAdapter(this);
        binding.classesRecyclerView.setAdapter(classAdapter);
        binding.classesRecyclerView.setNestedScrollingEnabled(false);

        // Setup swipe to delete
        setupSwipeToDeleteClass();

        binding.hamburgerIcon.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        binding.createClassButton.setOnClickListener(v -> openCreateClass());
        binding.ctaButton.setOnClickListener(v -> openCreateClass());

        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();

        loadClasses();
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
                                startActivity(new Intent(TeacherHome.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(TeacherHome.this, TeacherHome.class));
                            } else {
                                Toast.makeText(TeacherHome.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(TeacherHome.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(TeacherHome.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(TeacherHome.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(TeacherHome.this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClasses();
    }

    private void openCreateClass() {
        Intent intent = new Intent(TeacherHome.this, CreateClass.class);
        startActivity(intent);
    }

    private void loadClasses() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users")
                .document(userId)
                .collection("classes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    classList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ClassModel classModel = document.toObject(ClassModel.class);
                        classModel.setId(document.getId());
                        classList.add(classModel);
                    }

                    if (classList.isEmpty()) {
                        showEmptyState();
                    } else {
                        showClasses(classList);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TeacherHome.this, "Error loading classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void showEmptyState() {
        binding.emptyStateLayout.setVisibility(View.VISIBLE);
        binding.classesRecyclerView.setVisibility(View.GONE);
    }

    private void showClasses(List<ClassModel> classes) {
        binding.emptyStateLayout.setVisibility(View.GONE);
        binding.classesRecyclerView.setVisibility(View.VISIBLE);
        classAdapter.setClasses(classes);
    }

    @Override
    public void onClassClick(ClassModel classModel) {
        Intent intent = new Intent(TeacherHome.this, ClassInformation.class);
        intent.putExtra("CLASS_MODEL", classModel);
        startActivity(intent);
    }

    private void setupSwipeToDeleteClass() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.parseColor("#C92A2A"));
            private final Drawable deleteIcon = ContextCompat.getDrawable(TeacherHome.this, android.R.drawable.ic_menu_delete);

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ClassModel classItem = classAdapter.getClassAt(position);

                if (classItem != null) {
                    RemoveClassConfirmationDialog confirmDialog = new RemoveClassConfirmationDialog(
                            TeacherHome.this,
                            classItem.getClassName(),
                            () -> {
                                removeClassFromUser(classItem, position);
                            },
                            () -> {
                                classAdapter.notifyItemChanged(position);
                            }
                    );
                    confirmDialog.show();
                } else {
                    classAdapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                int backgroundCornerOffset = 20;

                if (dX < 0) {
                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                    int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                    background.setBounds(
                            itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                            itemView.getTop(),
                            itemView.getRight(),
                            itemView.getBottom()
                    );

                    background.draw(c);
                    deleteIcon.draw(c);
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.classesRecyclerView);
    }

    private void removeClassFromUser(ClassModel classItem, int position) {
        if (classItem == null || classItem.getId() == null) {
            Log.e(TAG, "Class item or ID is null");
            Toast.makeText(TeacherHome.this, "Error: Invalid class data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate position
        if (position < 0 || position >= classList.size()) {
            Log.e(TAG, "Invalid position: " + position + ", list size: " + classList.size());
            Toast.makeText(TeacherHome.this, "Error: Invalid position", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = mAuth.getCurrentUser().getUid();
        String classId = classItem.getId();

        Log.d(TAG, "Starting removal of class at position " + position + ": " + classItem.getClassName());

        // Remove from teacher's classes collection
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✓ Removed from user's classes");

                    // Remove from allClasses collection
                    db.collection("allClasses")
                            .document(classId)
                            .delete()
                            .addOnSuccessListener(unused2 -> {
                                Log.d(TAG, "✓ Removed from allClasses");

                                // Remove all students' enrollment references
                                removeStudentEnrollments(classId, () -> {
                                    try {
                                        // Update UI - only update if position is still valid
                                        if (position >= 0 && position < classList.size()) {
                                            classList.remove(position);
                                            classAdapter.notifyItemRemoved(position);
                                            classAdapter.notifyItemRangeChanged(position, classList.size());
                                            Log.d(TAG, "✓ Updated adapter at position " + position);
                                        } else {
                                            Log.w(TAG, "Position no longer valid, reloading classes");
                                            loadClasses();
                                            return;
                                        }

                                        if (classList.isEmpty()) {
                                            showEmptyState();
                                        }

                                        Toast.makeText(TeacherHome.this, "Class removed successfully", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "✓ Class " + classItem.getClassName() + " removed successfully");
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error updating adapter", e);
                                        loadClasses();
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to remove from allClasses", e);
                                Toast.makeText(TeacherHome.this, "Failed to remove class", Toast.LENGTH_SHORT).show();
                                try {
                                    if (position >= 0 && position < classList.size()) {
                                        classAdapter.notifyItemChanged(position);
                                    }
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error notifying adapter", ex);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove class", e);
                    Toast.makeText(TeacherHome.this, "Failed to remove class", Toast.LENGTH_SHORT).show();
                    try {
                        if (position >= 0 && position < classList.size()) {
                            classAdapter.notifyItemChanged(position);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error notifying adapter", ex);
                    }
                });
    }

    private void removeStudentEnrollments(String classId, Runnable onComplete) {
        Log.d(TAG, "Starting to remove student enrollments for class: " + classId);

        // Get all enrolled students and remove their enrollment reference
        db.collection("allClasses")
                .document(classId)
                .get()
                .addOnSuccessListener(classDoc -> {
                    if (classDoc.exists()) {
                        List<String> allowedEmails = (List<String>) classDoc.get("allowedStudentEmails");

                        if (allowedEmails != null && !allowedEmails.isEmpty()) {
                            final int[] completedCount = {0};
                            final int totalEmails = allowedEmails.size();

                            Log.d(TAG, "Found " + totalEmails + " students to unenroll");

                            for (String email : allowedEmails) {
                                db.collection("users")
                                        .whereEqualTo("schoolEmail", email)
                                        .limit(1)
                                        .get()
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                                String studentId = task.getResult().getDocuments().get(0).getId();
                                                db.collection("users")
                                                        .document(studentId)
                                                        .collection("enrolledClasses")
                                                        .document(classId)
                                                        .delete()
                                                        .addOnSuccessListener(unused -> {
                                                            Log.d(TAG, "✓ Unenrolled student: " + email);
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e(TAG, "Error unenrolling student: " + email, e);
                                                        });
                                            }

                                            completedCount[0]++;
                                            if (completedCount[0] == totalEmails) {
                                                Log.d(TAG, "✓ All students unenrolled");
                                                onComplete.run();
                                            }
                                        });
                            }
                        } else {
                            Log.d(TAG, "No students enrolled in this class");
                            onComplete.run();
                        }
                    } else {
                        Log.e(TAG, "Class document not found");
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing student enrollments", e);
                    onComplete.run();
                });
    }
}