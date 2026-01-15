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
        classAdapter.setClasses(archivedClasses);
    }

    private void setupSwipeToUnarchive() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            private final ColorDrawable background = new ColorDrawable(Color.parseColor("#2EAD00"));
            private final Drawable unarchiveIcon = ContextCompat.getDrawable(ArchiveActivity.this, R.drawable.ic_unarchive);

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
                    unarchiveClass(classItem, position);
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

                if (dX > 0) {
                    int iconMargin = (itemView.getHeight() - (unarchiveIcon != null ? unarchiveIcon.getIntrinsicHeight() : 0)) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - (unarchiveIcon != null ? unarchiveIcon.getIntrinsicHeight() : 0)) / 2;
                    int iconBottom = iconTop + (unarchiveIcon != null ? unarchiveIcon.getIntrinsicHeight() : 0);
                    int iconLeft = itemView.getLeft() + iconMargin;
                    int iconRight = iconLeft + (unarchiveIcon != null ? unarchiveIcon.getIntrinsicWidth() : 0);

                    if (unarchiveIcon != null) {
                        unarchiveIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    }

                    background.setBounds(
                            itemView.getLeft(),
                            itemView.getTop(),
                            itemView.getLeft() + ((int) dX) + backgroundCornerOffset,
                            itemView.getBottom()
                    );

                    background.draw(c);
                    if (unarchiveIcon != null) {
                        unarchiveIcon.draw(c);
                    }
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.archivedClassesRecyclerView);
    }

    private void loadArchivedClasses() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            showEmptyState();
            return;
        }

        String userId = currentUser.getUid();

        Log.d(TAG, "Loading archived classes for user: " + userId);

        db.collection("users")
                .document(userId)
                .collection("classes")
                .whereEqualTo("isArchived", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    archivedClasses.clear();
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " archived classes");

                    for (var document : queryDocumentSnapshots) {
                        ClassModel classModel = document.toObject(ClassModel.class);
                        classModel.setId(document.getId());
                        archivedClasses.add(classModel);
                        Log.d(TAG, "Added archived class: " + classModel.getClassName());
                    }

                    classAdapter.setClasses(archivedClasses);
                    updateUI();

                    if (!archivedClasses.isEmpty()) {
                        Log.d(TAG, "✓ Loaded " + archivedClasses.size() + " archived classes");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading archived classes", e);
                    Toast.makeText(ArchiveActivity.this, "Error loading archived classes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void unarchiveClass(ClassModel classItem, int position) {
        String teacherId = mAuth.getCurrentUser().getUid();
        String classId = classItem.getId();

        Log.d(TAG, "Unarchiving class: " + classItem.getClassName());

        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("isArchived", false)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "✓ Updated teacher's classes");

                    db.collection("allClasses")
                            .document(classId)
                            .update("isArchived", false)
                            .addOnSuccessListener(unused2 -> {
                                Log.d(TAG, "✓ Updated allClasses");

                                archivedClasses.remove(position);
                                classAdapter.notifyItemRemoved(position);
                                classAdapter.notifyItemRangeChanged(position, archivedClasses.size());

                                updateUI();
                                Toast.makeText(ArchiveActivity.this, "Class unarchived successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to unarchive in allClasses", e);
                                classAdapter.notifyItemChanged(position);
                                Toast.makeText(ArchiveActivity.this, "Failed to unarchive class", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to unarchive in teacher's classes", e);
                    classAdapter.notifyItemChanged(position);
                    Toast.makeText(ArchiveActivity.this, "Failed to unarchive class", Toast.LENGTH_SHORT).show();
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
        // Allow viewing archived class details
        Intent intent = new Intent(ArchiveActivity.this, ClassInformation.class);
        intent.putExtra("CLASS_MODEL", classModel);
        startActivity(intent);
    }
}