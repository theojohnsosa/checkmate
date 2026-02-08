package com.example.authtest;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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

    private ActivityTeacherHomeBinding binding;
    private ClassAdapter classAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private List<ClassModel> classList = new ArrayList<>();
    private List<ClassModel> filteredClasses = new ArrayList<>();
    private EditText classSearchBar;
    private ImageView clearSearchButton;
    private Map<ClassModel, String> classIdMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTeacherHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        classSearchBar = findViewById(R.id.classSearchBar);
        clearSearchButton = findViewById(R.id.clearSearchButton);

        binding.classesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        classAdapter = new ClassAdapter(this);
        binding.classesRecyclerView.setAdapter(classAdapter);
        binding.classesRecyclerView.setNestedScrollingEnabled(false);

        setupSwipeToDeleteClass();

        binding.hamburgerIcon.setOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        binding.createClassButton.setOnClickListener(view -> {
            openCreateClass();
        });

        binding.ctaButton.setOnClickListener(view -> {
            openCreateClass();
        });

        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();
        setupClassSearch();
        checkUserTypeAndConfigureMenu();
        loadClasses();

        navigationView.getMenu().findItem(R.id.menu_streak).setVisible(false);
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
                startActivity(new Intent(TeacherHome.this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent streakIntent = new Intent(TeacherHome.this, AttendanceStreak.class);
                startActivity(streakIntent);
                return true;
            }  else if (itemId == R.id.menu_leaderboards) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(TeacherHome.this, Leaderboards.class));
                return true;
            }else if (itemId == R.id.menu_attendance_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(TeacherHome.this, AttendanceHistoryActivity.class));
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(TeacherHome.this, ArchiveActivity.class));
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

    private void setupClassSearch() {
        classSearchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchQuery = s.toString().trim().toLowerCase();
                filterClasses(searchQuery);

                if (searchQuery.isEmpty()) {
                    clearSearchButton.setVisibility(View.GONE);
                } else {
                    clearSearchButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        clearSearchButton.setOnClickListener(view -> {
            classSearchBar.setText("");
            filteredClasses.clear();
            classAdapter.setClasses(new ArrayList<>(classList));
        });
    }

    private void filterClasses(String searchQuery) {
        filteredClasses.clear();

        if (searchQuery.isEmpty()) {
            classAdapter.setClasses(new ArrayList<>(classList));
            return;
        }

        for (ClassModel classModel : classList) {
            String className = classModel.getClassName() != null ? classModel.getClassName().toLowerCase() : "";
            String classCode = classModel.getClassCode() != null ? classModel.getClassCode().toLowerCase() : "";
            String subjectCode = classModel.getSubjectCode() != null ? classModel.getSubjectCode().toLowerCase() : "";

            if (className.contains(searchQuery) ||
                    classCode.contains(searchQuery) ||
                    subjectCode.contains(searchQuery)) {
                filteredClasses.add(classModel);
            }
        }

        classAdapter.setClasses(new ArrayList<>(filteredClasses));
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
                    classIdMap.clear();
                    int archivedCount = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ClassModel classModel = document.toObject(ClassModel.class);
                        classIdMap.put(classModel, document.getId());

                        Boolean isArchived = document.getBoolean("isArchived");

                        if (isArchived == null || !isArchived) {
                            classList.add(classModel);
                        } else {
                            archivedCount++;
                        }
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
        binding.searchBarContainer.setVisibility(View.GONE);
    }

    private void showClasses(List<ClassModel> classes) {
        binding.emptyStateLayout.setVisibility(View.GONE);
        binding.classesRecyclerView.setVisibility(View.VISIBLE);
        binding.searchBarContainer.setVisibility(View.VISIBLE);
        classAdapter.setClasses(classes);
    }

    @Override
    public void onClassClick(ClassModel classModel) {
        Intent intent = new Intent(TeacherHome.this, ClassInformation.class);
        intent.putExtra("CLASS_MODEL", classModel);

        String documentId = classIdMap.get(classModel);
        if (documentId != null) {
            intent.putExtra("CLASS_ID", documentId);
        }

        intent.putExtra("CLASS_CODE", classModel.getClassCode());
        startActivity(intent);
    }

    private void setupSwipeToDeleteClass() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            private final ColorDrawable archiveBackground = new ColorDrawable(Color.parseColor("#FF8C00"));
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
        if (classItem == null) {
            Toast.makeText(TeacherHome.this, "Error: Invalid class data", Toast.LENGTH_SHORT).show();
            return;
        }

        if (position < 0 || position >= classList.size()) {
            Toast.makeText(TeacherHome.this, "Error: Invalid position", Toast.LENGTH_SHORT).show();
            return;
        }

        String classId = classIdMap.get(classItem);
        if (classId == null) {
            Toast.makeText(TeacherHome.this, "Error: Class ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String teacherId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(teacherId)
                .collection("classes")
                .document(classId)
                .update("isArchived", true)
                .addOnSuccessListener(unused -> {
                    db.collection("allClasses")
                            .document(classId)
                            .update("isArchived", true)
                            .addOnSuccessListener(unused2 -> {
                                try {
                                    if (position >= 0 && position < classList.size()) {
                                        ClassModel removed = classList.remove(position);
                                        classIdMap.remove(removed);
                                        classAdapter.notifyItemRemoved(position);
                                        classAdapter.notifyItemRangeChanged(position, classList.size());
                                    } else {
                                        loadClasses();
                                        return;
                                    }

                                    if (classList.isEmpty()) {
                                        showEmptyState();
                                    }

                                    Toast.makeText(TeacherHome.this, "Class archived successfully", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    loadClasses();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(TeacherHome.this, "Failed to archive class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                try {
                                    if (position >= 0 && position < classList.size()) {
                                        classAdapter.notifyItemChanged(position);
                                    }
                                } catch (Exception ex) {

                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TeacherHome.this, "Failed to archive class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    try {
                        if (position >= 0 && position < classList.size()) {
                            classAdapter.notifyItemChanged(position);
                        }
                    } catch (Exception ex) {
                    }
                });
    }
}