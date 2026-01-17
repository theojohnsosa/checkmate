package com.example.authtest;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import com.example.authtest.databinding.ActivityArchiveBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ArchiveActivity extends AppCompatActivity implements ClassAdapter.OnClassClickListener {

    private static final String TAG = "ArchiveActivity";
    private ActivityArchiveBinding binding;
    private ClassAdapter classAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private List<ClassModel> archivedClasses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityArchiveBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        binding.hamburgerIcon.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        binding.backButton.setOnClickListener(v -> finish());

        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();
        setupRecyclerView();
        setupSwipeToUnarchive();

        Log.d(TAG, "onCreate completed, calling loadArchivedClasses()");
        loadArchivedClasses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called, reloading archived classes");
        loadArchivedClasses();
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
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
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
                                startActivity(new Intent(ArchiveActivity.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(ArchiveActivity.this, TeacherHome.class));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(ArchiveActivity.this, "Error loading user info", Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(ArchiveActivity.this, SignIn.class);
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

    private void setupRecyclerView() {
        classAdapter = new ClassAdapter(this);
        binding.archivedClassesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.archivedClassesRecyclerView.setAdapter(classAdapter);
        binding.archivedClassesRecyclerView.setNestedScrollingEnabled(false);
    }

    private void setupSwipeToUnarchive() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final ColorDrawable unarchiveBackground = new ColorDrawable(Color.parseColor("#2EAD00"));
            private final ColorDrawable deleteBackground = new ColorDrawable(Color.parseColor("#C92A2A"));
            private final Drawable unarchiveIcon = ContextCompat.getDrawable(ArchiveActivity.this, R.drawable.ic_unarchive);
            private final Drawable deleteIcon = ContextCompat.getDrawable(ArchiveActivity.this, android.R.drawable.ic_menu_delete);

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

                if (direction == ItemTouchHelper.RIGHT) {
                    Log.d(TAG, "Swiped RIGHT - unarchive class");
                    unarchiveClass(classItem, position);
                }
                else if (direction == ItemTouchHelper.LEFT) {
                    RemoveClassConfirmationDialog confirmDialog = new RemoveClassConfirmationDialog(
                            ArchiveActivity.this,
                            classItem.getClassName(),
                            () -> {
                                deleteClassFromArchive(classItem, position);
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

                if (dX > 0) {
                    int iconMargin = (itemView.getHeight() - (unarchiveIcon != null ? unarchiveIcon.getIntrinsicHeight() : 0)) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - (unarchiveIcon != null ? unarchiveIcon.getIntrinsicHeight() : 0)) / 2;
                    int iconBottom = iconTop + (unarchiveIcon != null ? unarchiveIcon.getIntrinsicHeight() : 0);
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconRight = iconLeft + (unarchiveIcon != null ? unarchiveIcon.getIntrinsicWidth() : 0);

                    if (unarchiveIcon != null) {
                        unarchiveIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    }

                    unarchiveBackground.setBounds(
                            itemView.getLeft(),
                            itemView.getTop(),
                            itemView.getLeft() + ((int) dX) + backgroundCornerOffset,
                            itemView.getBottom()
                    );

                    unarchiveBackground.draw(c);
                    if (unarchiveIcon != null) {
                        unarchiveIcon.draw(c);
                    }
                }
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
        itemTouchHelper.attachToRecyclerView(binding.archivedClassesRecyclerView);
    }

    private void deleteClassFromArchive(ClassModel classItem, int position) {
        if (classItem == null || classItem.getId() == null) {
            Toast.makeText(ArchiveActivity.this, "Error: Invalid class data", Toast.LENGTH_SHORT).show();
            return;
        }

        if (position < 0 || position >= archivedClasses.size()) {
            Toast.makeText(ArchiveActivity.this, "Error: Invalid position", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String classId = classItem.getId();

        db.collection("users")
                .document(userId)
                .collection("classes")
                .document(classId)
                .delete()
                .addOnSuccessListener(unused -> {
                    db.collection("allClasses")
                            .document(classId)
                            .delete()
                            .addOnSuccessListener(unused2 -> {
                                removeStudentEnrollmentsFromArchive(classId, () -> {
                                    try {
                                        if (position >= 0 && position < archivedClasses.size()) {
                                            archivedClasses.remove(position);
                                            classAdapter.notifyItemRemoved(position);
                                            classAdapter.notifyItemRangeChanged(position, archivedClasses.size());
                                        } else {
                                            loadArchivedClasses();
                                            return;
                                        }

                                        if (archivedClasses.isEmpty()) {
                                            showEmptyState();
                                        }

                                        Toast.makeText(ArchiveActivity.this, "Class deleted permanently", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        loadArchivedClasses();
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ArchiveActivity.this, "Failed to delete class", Toast.LENGTH_SHORT).show();
                                try {
                                    if (position >= 0 && position < archivedClasses.size()) {
                                        classAdapter.notifyItemChanged(position);
                                    }
                                } catch (Exception ex) {

                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ArchiveActivity.this, "Failed to delete class", Toast.LENGTH_SHORT).show();
                    try {
                        if (position >= 0 && position < archivedClasses.size()) {
                            classAdapter.notifyItemChanged(position);
                        }
                    } catch (Exception ex) {

                    }
                });
    }

    private void removeStudentEnrollmentsFromArchive(String classId, Runnable onComplete) {
        db.collection("allClasses")
                .document(classId)
                .get()
                .addOnSuccessListener(classDoc -> {
                    if (classDoc.exists()) {
                        List<String> allowedEmails = (List<String>) classDoc.get("allowedStudentEmails");

                        if (allowedEmails != null && !allowedEmails.isEmpty()) {
                            final int[] completedCount = {0};
                            final int totalEmails = allowedEmails.size();

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
                                                        .delete();
                                            }

                                            completedCount[0]++;
                                            if (completedCount[0] == totalEmails) {
                                                onComplete.run();
                                            }
                                        });
                            }
                        } else {
                            onComplete.run();
                        }
                    } else {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    onComplete.run();
                });
    }

    private void loadArchivedStudentClasses(String userId) {
        db.collection("users")
                .document(userId)
                .collection("enrolledClasses")
                .whereEqualTo("isArchived", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    archivedClasses.clear();

                    final int totalClasses = queryDocumentSnapshots.size();
                    final int[] loadedClasses = {0};

                    if (totalClasses == 0) {
                        classAdapter.setClasses(archivedClasses);
                        classAdapter.notifyDataSetChanged();
                        updateUI();
                        return;
                    }

                    List<ClassModel> tempClasses = new ArrayList<>();

                    for (var document : queryDocumentSnapshots) {
                        String classId = document.getString("classId");

                        if (classId != null) {
                            db.collection("allClasses")
                                    .document(classId)
                                    .get()
                                    .addOnSuccessListener(classDoc -> {
                                        if (classDoc.exists()) {
                                            ClassModel classModel = classDoc.toObject(ClassModel.class);
                                            if (classModel != null) {
                                                classModel.setId(classDoc.getId());
                                                tempClasses.add(classModel);
                                            }
                                        }

                                        loadedClasses[0]++;
                                        if (loadedClasses[0] == totalClasses) {
                                            archivedClasses.clear();
                                            archivedClasses.addAll(tempClasses);

                                            classAdapter.setClasses(archivedClasses);
                                            classAdapter.notifyDataSetChanged();

                                            updateUI();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        loadedClasses[0]++;
                                        if (loadedClasses[0] == totalClasses) {
                                            archivedClasses.clear();
                                            archivedClasses.addAll(tempClasses);

                                            classAdapter.setClasses(archivedClasses);
                                            classAdapter.notifyDataSetChanged();
                                            updateUI();
                                        }
                                    });
                        } else {
                            loadedClasses[0]++;
                            if (loadedClasses[0] == totalClasses) {
                                archivedClasses.clear();
                                archivedClasses.addAll(tempClasses);
                                classAdapter.setClasses(archivedClasses);
                                classAdapter.notifyDataSetChanged();
                                updateUI();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ArchiveActivity.this, "Error loading archived classes", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void unarchiveClass(ClassModel classItem, int position) {
        String userId = mAuth.getCurrentUser().getUid();
        String classId = classItem.getId();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String userType = userDoc.getString("userType");

                        if ("Teacher".equalsIgnoreCase(userType)) {
                            unarchiveTeacherClass(userId, classId, position);
                        } else if ("Student".equalsIgnoreCase(userType)) {
                            unarchiveStudentClass(userId, classId, position);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    classAdapter.notifyItemChanged(position);
                    Toast.makeText(ArchiveActivity.this, "Failed to unarchive class", Toast.LENGTH_SHORT).show();
                });
    }

    private void unarchiveTeacherClass(String teacherId, String classId, int position) {
        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("isArchived", false)
                .addOnSuccessListener(unused -> {
                    db.collection("allClasses")
                            .document(classId)
                            .update("isArchived", false)
                            .addOnSuccessListener(unused2 -> {
                                archivedClasses.remove(position);
                                classAdapter.notifyItemRemoved(position);
                                classAdapter.notifyItemRangeChanged(position, archivedClasses.size());

                                updateUI();
                                Toast.makeText(ArchiveActivity.this, "Class unarchived successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                classAdapter.notifyItemChanged(position);
                                Toast.makeText(ArchiveActivity.this, "Failed to unarchive class", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    classAdapter.notifyItemChanged(position);
                    Toast.makeText(ArchiveActivity.this, "Failed to unarchive class", Toast.LENGTH_SHORT).show();
                });
    }

    private void unarchiveStudentClass(String studentId, String classId, int position) {
        db.collection("users")
                .document(studentId)
                .collection("enrolledClasses")
                .document(classId)
                .update("isArchived", false)
                .addOnSuccessListener(unused -> {
                    archivedClasses.remove(position);
                    classAdapter.notifyItemRemoved(position);
                    classAdapter.notifyItemRangeChanged(position, archivedClasses.size());

                    updateUI();
                    Toast.makeText(ArchiveActivity.this, "Class unarchived successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    classAdapter.notifyItemChanged(position);
                    Toast.makeText(ArchiveActivity.this, "Failed to unarchive class", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadArchivedClasses() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String userType = userDoc.getString("userType");

                        if ("Teacher".equalsIgnoreCase(userType)) {
                            loadArchivedTeacherClasses(userId);
                        } else if ("Student".equalsIgnoreCase(userType)) {
                            loadArchivedStudentClasses(userId);
                        } else {
                            showEmptyState();
                        }
                    } else {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    showEmptyState();
                });
    }

    private void loadArchivedTeacherClasses(String userId) {
        db.collection("users")
                .document(userId)
                .collection("classes")
                .whereEqualTo("isArchived", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    archivedClasses.clear();

                    for (var document : queryDocumentSnapshots) {
                        ClassModel classModel = document.toObject(ClassModel.class);
                        classModel.setId(document.getId());
                        archivedClasses.add(classModel);
                    }

                    classAdapter.setClasses(archivedClasses);
                    classAdapter.notifyDataSetChanged();

                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ArchiveActivity.this, "Error loading archived classes", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void updateUI() {
        boolean isEmpty = archivedClasses.isEmpty();

        binding.emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.archivedClassesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState() {
        binding.emptyStateLayout.setVisibility(View.VISIBLE);
        binding.archivedClassesRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onClassClick(ClassModel classModel) {
        Intent intent = new Intent(ArchiveActivity.this, ClassInformation.class);
        intent.putExtra("CLASS_MODEL", classModel);
        startActivity(intent);
    }
}