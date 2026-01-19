package com.example.authtest;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        binding.hamburgerIcon.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        setupNavigationDrawer();

        loadUserInfoInDrawer();

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

        setupSwipeToArchiveClass();

        classAdapter.setClasses(classList);
    }

    private void setupSwipeToArchiveClass() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            private final ColorDrawable archiveBackground = new ColorDrawable(Color.parseColor("#FF8C00"));
            private final Drawable archiveIcon = ContextCompat.getDrawable(StudentHome.this, R.drawable.ic_archive);

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
                    ArchiveClassConfirmationDialog confirmDialog = new ArchiveClassConfirmationDialog(
                            StudentHome.this,
                            classItem.getClassName(),
                            () -> {
                                archiveClass(classItem, position);
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
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.classesRecyclerView);
    }

    private void archiveClass(ClassModel classItem, int position) {
        if (classItem == null || classItem.getId() == null) {
            Toast.makeText(StudentHome.this, "Error: Invalid class data", Toast.LENGTH_SHORT).show();
            return;
        }

        if (position < 0 || position >= classList.size()) {
            Toast.makeText(StudentHome.this, "Error: Invalid position", Toast.LENGTH_SHORT).show();
            return;
        }

        String studentId = mAuth.getCurrentUser().getUid();
        String classId = classItem.getId();

        db.collection("users")
                .document(studentId)
                .collection("enrolledClasses")
                .document(classId)
                .update("isArchived", true)
                .addOnSuccessListener(unused -> {
                    try {
                        if (position >= 0 && position < classList.size()) {
                            classList.remove(position);
                            classAdapter.notifyItemRemoved(position);
                            classAdapter.notifyItemRangeChanged(position, classList.size());
                        } else {
                            loadClasses();
                            return;
                        }
                    } catch (Exception e) {
                        loadClasses();
                        return;
                    }

                    updateUI();
                    Toast.makeText(StudentHome.this, "Class archived successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StudentHome.this, "Failed to archive class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    try {
                        if (position >= 0 && position < classList.size()) {
                            classAdapter.notifyItemChanged(position);
                        }
                    } catch (Exception ex) {
                    }
                });
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
                startActivity(new Intent(StudentHome.this, ArchiveActivity.class));
                return true;
            } else if (itemId == R.id.menu_settings) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show();
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

                        Boolean isArchived = doc.getBoolean("isArchived");

                        if (isArchived != null && isArchived) {
                            loadedClasses[0]++;
                            if (loadedClasses[0] == totalClasses) {
                                classAdapter.notifyDataSetChanged();
                                updateUI();
                                isLoadingClasses = false;
                            }
                            continue;
                        }

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