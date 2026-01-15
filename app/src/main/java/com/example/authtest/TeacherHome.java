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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Streak feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(TeacherHome.this, ArchiveActivity.class));
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

        Log.d(TAG, "Loading classes for user: " + userId);

        db.collection("users")
                .document(userId)
                .collection("classes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    classList.clear();
                    int archivedCount = 0;

                    Log.d(TAG, "Total classes found: " + queryDocumentSnapshots.size());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ClassModel classModel = document.toObject(ClassModel.class);
                        classModel.setId(document.getId());

                        // Get the isArchived field directly from Firestore document
                        Boolean isArchived = document.getBoolean("isArchived");

                        Log.d(TAG, "Class: " + classModel.getClassName() +
                                ", ID: " + document.getId() +
                                ", isArchived (from doc): " + isArchived);

                        // Only include classes that are NOT archived
                        // isArchived should be false or null (missing)
                        if (isArchived == null || !isArchived) {
                            classList.add(classModel);
                            Log.d(TAG, "  → Added to display list");
                        } else {
                            archivedCount++;
                            Log.d(TAG, "  → FILTERED OUT (archived)");
                        }
                    }

                    Log.d(TAG, "Final result: " + classList.size() + " active classes, " + archivedCount + " archived");

                    if (classList.isEmpty()) {
                        showEmptyState();
                    } else {
                        showClasses(classList);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading classes: " + e.getMessage());
                    Toast.makeText(TeacherHome.this, "Error loading classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void fixMissingFields() {
        String teacherId = mAuth.getCurrentUser().getUid();

        Log.d(TAG, "Checking for classes with missing fields");

        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot) {
                        String classId = doc.getId();

                        db.collection("allClasses")
                                .document(classId)
                                .get()
                                .addOnSuccessListener(classDoc -> {
                                    if (classDoc.exists()) {
                                        Object teacherIdField = classDoc.get("teacherId");
                                        Object isArchivedField = classDoc.get("isArchived");

                                        Map<String, Object> updates = new HashMap<>();
                                        boolean needsUpdate = false;

                                        // Fix missing teacherId
                                        if (teacherIdField == null || teacherIdField.toString().isEmpty()) {
                                            Log.d(TAG, "Adding missing teacherId for class: " + classId);
                                            updates.put("teacherId", teacherId);
                                            needsUpdate = true;
                                        }

                                        // Fix missing isArchived - set to false for existing classes
                                        if (isArchivedField == null) {
                                            Log.d(TAG, "Adding missing isArchived for class: " + classId);
                                            updates.put("isArchived", false);
                                            needsUpdate = true;
                                        }

                                        // Perform the update if needed
                                        if (needsUpdate) {
                                            db.collection("allClasses")
                                                    .document(classId)
                                                    .update(updates)
                                                    .addOnSuccessListener(unused -> {
                                                        Log.d(TAG, "✓ Fixed fields for class: " + classId);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "Failed to fix fields for class: " + classId, e);
                                                    });
                                        }
                                    }
                                });
                    }
                });
    }

    private void fixMissingTeacherIds() {
        String teacherId = mAuth.getCurrentUser().getUid();

        Log.d(TAG, "Checking for classes with missing teacherId");

        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot) {
                        String classId = doc.getId();

                        db.collection("allClasses")
                                .document(classId)
                                .get()
                                .addOnSuccessListener(classDoc -> {
                                    if (classDoc.exists()) {
                                        Object teacherIdField = classDoc.get("teacherId");

                                        if (teacherIdField == null || teacherIdField.toString().isEmpty()) {
                                            Log.d(TAG, "Fixing missing teacherId for class: " + classId);

                                            db.collection("allClasses")
                                                    .document(classId)
                                                    .update("teacherId", teacherId)
                                                    .addOnSuccessListener(unused -> {
                                                        Log.d(TAG, "✓ Fixed teacherId for class: " + classId);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "Failed to fix teacherId for class: " + classId, e);
                                                    });
                                        }
                                    }
                                });
                    }
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
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final ColorDrawable deleteBackground = new ColorDrawable(Color.parseColor("#C92A2A"));
            private final ColorDrawable archiveBackground = new ColorDrawable(Color.parseColor("#FF8C00"));
            private final Drawable deleteIcon = ContextCompat.getDrawable(TeacherHome.this, android.R.drawable.ic_menu_delete);
            private final Drawable archiveIcon = ContextCompat.getDrawable(TeacherHome.this, R.drawable.ic_archive);

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ClassModel classItem = classAdapter.getClassAt(position);

                if (classItem == null) {
                    classAdapter.notifyItemChanged(position);
                    return;
                }

                // SWIPE RIGHT = Archive
                if (direction == ItemTouchHelper.RIGHT) {
                    ArchiveClassConfirmationDialog confirmDialog = new ArchiveClassConfirmationDialog(
                            TeacherHome.this,
                            classItem.getClassName(),
                            () -> {
                                archiveClass(classItem, position);
                            },
                            () -> {
                                classAdapter.notifyItemChanged(position);
                            }
                    );
                    confirmDialog.show();
                }
                // SWIPE LEFT = Delete
                else if (direction == ItemTouchHelper.LEFT) {
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
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                int backgroundCornerOffset = 20;

                // Swipe RIGHT = Archive (Orange background)
                if (dX > 0) {
                    int iconMargin = (itemView.getHeight() - (archiveIcon != null ? archiveIcon.getIntrinsicHeight() : 0)) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - (archiveIcon != null ? archiveIcon.getIntrinsicHeight() : 0)) / 2;
                    int iconBottom = iconTop + (archiveIcon != null ? archiveIcon.getIntrinsicHeight() : 0);
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconRight = iconLeft + (archiveIcon != null ? archiveIcon.getIntrinsicWidth() : 0);

                    if (archiveIcon != null) {
                        archiveIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    }

                    archiveBackground.setBounds(
                            itemView.getLeft(),
                            itemView.getTop(),
                            itemView.getLeft() + ((int) dX) + backgroundCornerOffset,
                            itemView.getBottom()
                    );

                    archiveBackground.draw(c);
                    if (archiveIcon != null) {
                        archiveIcon.draw(c);
                    }
                }
                // Swipe LEFT = Delete (Red background)
                else if (dX < 0) {
                    int iconMargin = (itemView.getHeight() - (deleteIcon != null ? deleteIcon.getIntrinsicHeight() : 0)) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - (deleteIcon != null ? deleteIcon.getIntrinsicHeight() : 0)) / 2;
                    int iconBottom = iconTop + (deleteIcon != null ? deleteIcon.getIntrinsicHeight() : 0);
                    int iconLeft = itemView.getRight() - iconMargin - (deleteIcon != null ? deleteIcon.getIntrinsicWidth() : 0);
                    int iconRight = itemView.getRight() - iconMargin;

                    if (deleteIcon != null) {
                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    }

                    deleteBackground.setBounds(
                            itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                            itemView.getTop(),
                            itemView.getRight(),
                            itemView.getBottom()
                    );

                    deleteBackground.draw(c);
                    if (deleteIcon != null) {
                        deleteIcon.draw(c);
                    }
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.classesRecyclerView);
    }

    private void archiveClass(ClassModel classItem, int position) {
        if (classItem == null || classItem.getId() == null) {
            Log.e(TAG, "Class item or ID is null");
            Toast.makeText(TeacherHome.this, "Error: Invalid class data", Toast.LENGTH_SHORT).show();
            return;
        }

        if (position < 0 || position >= classList.size()) {
            Log.e(TAG, "Invalid position: " + position + ", list size: " + classList.size());
            Toast.makeText(TeacherHome.this, "Error: Invalid position", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = mAuth.getCurrentUser().getUid();
        String classId = classItem.getId();

        Log.d(TAG, "Archiving class: " + classItem.getClassName() + " (ID: " + classId + ")");

        // Step 1: Update in teacher's personal classes collection
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("isArchived", true)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✓ Updated isArchived in user's classes");

                    // Step 2: Update in allClasses collection
                    db.collection("allClasses")
                            .document(classId)
                            .update("isArchived", true)
                            .addOnSuccessListener(unused2 -> {
                                Log.d(TAG, "✓ Updated isArchived in allClasses");

                                // Step 3: Remove from local list and refresh UI
                                try {
                                    if (position >= 0 && position < classList.size()) {
                                        classList.remove(position);
                                        classAdapter.notifyItemRemoved(position);
                                        classAdapter.notifyItemRangeChanged(position, classList.size());
                                        Log.d(TAG, "✓ Removed from adapter at position " + position);
                                    } else {
                                        Log.w(TAG, "Position invalid after archive, reloading");
                                        loadClasses();
                                        return;
                                    }

                                    if (classList.isEmpty()) {
                                        showEmptyState();
                                    }

                                    Toast.makeText(TeacherHome.this, "Class archived successfully", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "✓ Class archived successfully");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error updating UI after archive", e);
                                    loadClasses();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update isArchived in allClasses: " + e.getMessage(), e);
                                Toast.makeText(TeacherHome.this, "Failed to archive class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Log.e(TAG, "Failed to update isArchived in user's classes: " + e.getMessage(), e);
                    Toast.makeText(TeacherHome.this, "Failed to archive class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    try {
                        if (position >= 0 && position < classList.size()) {
                            classAdapter.notifyItemChanged(position);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error notifying adapter", ex);
                    }
                });
    }

    private void removeClassFromUser(ClassModel classItem, int position) {
        if (classItem == null || classItem.getId() == null) {
            Log.e(TAG, "Class item or ID is null");
            Toast.makeText(TeacherHome.this, "Error: Invalid class data", Toast.LENGTH_SHORT).show();
            return;
        }

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