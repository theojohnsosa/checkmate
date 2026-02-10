package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Activity showing searchable/filterable FAQ list
public class Faqs extends AppCompatActivity {

    private EditText searchBar;
    private ImageView clearSearchButton;
    private RecyclerView recyclerView;
    private FaqAdapter adapter;
    private List<FaqItem> allFAQs;
    private List<FaqItem> filteredFAQs;
    private String currentFilter = "All";
    private LinearLayout emptyStateLayout;
    private Button filterAllButton;
    private Button filterGeneralButton;
    private Button filterAccountButton;
    private Button filterTeachersButton;
    private Button filterStudentsButton;
    private Button filterTroubleshootingButton;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AppCompatButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faqs);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        findViewById(R.id.hamburger_icon).setOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        initializeViews();
        setupBackButton();
        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();
        initializeFAQData();
        checkUserTypeAndConfigureMenu();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FaqAdapter(filteredFAQs);
        recyclerView.setAdapter(adapter);

        setupSearchFunctionality();
        setupFilterButtons();
    }

    private void initializeViews() {
        searchBar = findViewById(R.id.faqSearchBar);
        clearSearchButton = findViewById(R.id.clearSearchButton);
        recyclerView = findViewById(R.id.faqRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        filterAllButton = findViewById(R.id.filterAll);
        filterGeneralButton = findViewById(R.id.filterGeneral);
        filterAccountButton = findViewById(R.id.filterAccount);
        filterTeachersButton = findViewById(R.id.filterTeachers);
        filterStudentsButton = findViewById(R.id.filterStudents);
        filterTroubleshootingButton = findViewById(R.id.filterTroubleshooting);

        backButton = findViewById(R.id.backButton);
    }

    /*
         Creates ArrayList of FaqItem objects
         Manually adds 18 predefined FAQ items across 5 categories
         Categories: General Information, Account and Access, For Teachers, For Students, General Troubleshooting
     */
    private void initializeFAQData() {
        allFAQs = new ArrayList<>();

        allFAQs.add(new FaqItem(
                "What is Checkmate?",
                "Checkmate is an attendance tracking application designed to help educators and students manage classroom attendance efficiently. It provides real-time insights into attendance patterns and helps maintain accurate records.",
                "General Information"
        ));

        allFAQs.add(new FaqItem(
                "Do I need internet connection to use Checkmate?",
                "Checkmate requires an internet connection to sync data with the server. However, basic functionality may work offline with limited features. Internet connection is recommended for the best experience.",
                "Account and Access"
        ));

        allFAQs.add(new FaqItem(
                "What devices can I use Checkmate on?",
                "Checkmate is available on Android and iOS devices. You can use it on smartphones and tablets with these operating systems.",
                "Account and Access"
        ));

        allFAQs.add(new FaqItem(
                "How do I create an account?",
                "To create an account, download the Checkmate app and tap 'Sign Up'. Enter your email address, create a strong password, and fill in your personal information. You'll receive a verification email to confirm your account.",
                "Account and Access"
        ));

        allFAQs.add(new FaqItem(
                "What is the difference between a teacher account and a student account?",
                "Teacher accounts have the ability to create classes, manage attendance records, and view student analytics. Student accounts allow students to join classes and mark their attendance.",
                "Account and Access"
        ));

        allFAQs.add(new FaqItem(
                "How do I create a class in Checkmate?",
                "To create a class, navigate to the home screen and tap 'Create Class'. Enter the class name, subject, and other details. You can then invite students to join the class.",
                "For Teachers"
        ));

        allFAQs.add(new FaqItem(
                "How do I add students to my class?",
                "You can add students by sharing a class code or by manually inviting them via email. Students can join by entering the class code in the app.",
                "For Teachers"
        ));

        allFAQs.add(new FaqItem(
                "How do I enable attendance tracking for my class?",
                "In your class settings, toggle on 'Enable Attendance Tracking'. You can then customize the attendance window and other settings as needed.",
                "For Teachers"
        ));

        allFAQs.add(new FaqItem(
                "Can I set different attendance time limits for different classes?",
                "Yes, you can set different attendance times for each class individually in the class settings.",
                "For Teachers"
        ));

        allFAQs.add(new FaqItem(
                "How can I view my students' attendance records?",
                "Navigate to your class and tap on the 'Attendance' tab. You'll see a detailed report of each student's attendance records, including dates and times.",
                "For Teachers"
        ));

        allFAQs.add(new FaqItem(
                "What does the app mean by \"false\" check-in detection?",
                "False check-in detection is a security feature that identifies suspicious attendance patterns or locations that don't match the expected classroom location.",
                "For Teachers"
        ));

        allFAQs.add(new FaqItem(
                "How do I join a class?",
                "Ask your teacher for the class code. Open the app and tap 'Join Class', then enter the code to join.",
                "For Students"
        ));

        allFAQs.add(new FaqItem(
                "How do I mark my attendance?",
                "When your class attendance window is open, tap 'Mark Attendance' on the home screen. Your attendance will be recorded immediately.",
                "For Students"
        ));

        allFAQs.add(new FaqItem(
                "What happens if I miss the attendance window?",
                "If you miss the attendance window, you won't be able to mark attendance unless your teacher manually adjusts your record or extends the window.",
                "For Students"
        ));

        allFAQs.add(new FaqItem(
                "Is there a grace period for marking attendance?",
                "The grace period depends on your teacher's settings. Some classes may have a 5-10 minute grace period after the official attendance time.",
                "For Students"
        ));

        allFAQs.add(new FaqItem(
                "Can I check in if I don't have an internet connection?",
                "You need an active internet connection to mark attendance. The app will notify you if you're offline.",
                "For Students"
        ));

        allFAQs.add(new FaqItem(
                "I'm having trouble logging in. What should I do?",
                "If you're having trouble logging in, try resetting your password by tapping 'Forgot Password' on the login screen. If the issue persists, ensure your internet connection is active and your credentials are correct.",
                "General Troubleshooting"
        ));

        allFAQs.add(new FaqItem(
                "Why is my attendance not showing up?",
                "If your attendance is not showing up, ensure that you marked attendance during the designated window. Check with your teacher if there are any technical issues.",
                "General Troubleshooting"
        ));

        allFAQs.add(new FaqItem(
                "The app keeps crashing. How can I fix this?",
                "Try updating the app to the latest version. If the issue persists, uninstall and reinstall the app, or contact support for assistance.",
                "General Troubleshooting"
        ));

        filteredFAQs = new ArrayList<>(allFAQs);
    }

    /*
         Adds TextWatcher to search EditText
         Calls filterFAQs() whenever user types
         Shows clear button when search text is non-empty
     */
    private void setupSearchFunctionality() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchQuery = s.toString().toLowerCase();
                if (searchQuery.isEmpty()) {
                    clearSearchButton.setVisibility(View.GONE);
                } else {
                    clearSearchButton.setVisibility(View.VISIBLE);
                }
                filterFAQs(searchQuery);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        clearSearchButton.setOnClickListener(view -> {
            searchBar.setText("");
        });
    }

    /*
         Each category button calls applyFilter() with category name
         Updates button visual states to show which category is active
         Retrieves all FAQs when "All" button tapped
     */
    private void setupFilterButtons() {
        filterAllButton.setOnClickListener(view -> {
            applyFilter("All");
        });
        filterGeneralButton.setOnClickListener(view -> {
            applyFilter("General Information");
        });
        filterAccountButton.setOnClickListener(view -> {
            applyFilter("Account and Access");
        });
        filterTeachersButton.setOnClickListener(view -> {
            applyFilter("For Teachers");
        });
        filterStudentsButton.setOnClickListener(view -> {
            applyFilter("For Students");
        });
        filterTroubleshootingButton.setOnClickListener(view -> {
            applyFilter("General Troubleshooting");
        });
    }

    private void applyFilter(String category) {
        currentFilter = category;
        updateFilterButtonStates();
        filterFAQs(searchBar.getText().toString().toLowerCase());
    }

    private void updateFilterButtonStates() {
        setButtonInactive(filterAllButton);
        setButtonInactive(filterGeneralButton);
        setButtonInactive(filterAccountButton);
        setButtonInactive(filterTeachersButton);
        setButtonInactive(filterStudentsButton);
        setButtonInactive(filterTroubleshootingButton);

        switch (currentFilter) {
            case "All":
                setButtonActive(filterAllButton);
                break;
            case "General Information":
                setButtonActive(filterGeneralButton);
                break;
            case "Account and Access":
                setButtonActive(filterAccountButton);
                break;
            case "For Teachers":
                setButtonActive(filterTeachersButton);
                break;
            case "For Students":
                setButtonActive(filterStudentsButton);
                break;
            case "General Troubleshooting":
                setButtonActive(filterTroubleshootingButton);
                break;
        }
    }

    private void setButtonActive(Button button) {
        button.setBackgroundResource(R.drawable.filter_button_active);
        button.setTextColor(0xFF000000);
    }

    private void setButtonInactive(Button button) {
        button.setBackgroundResource(R.drawable.filter_button_inactive);
        button.setTextColor(0xFFFFFFFF);
    }

    /*
         Uses Java streams (filter/collect) to find matching items
         Checks both category match AND search text match
         Updates adapter with filtered list
         Shows empty state if no results
     */
    private void filterFAQs(String searchQuery) {
        filteredFAQs = allFAQs.stream()
                .filter(faq -> {
                    boolean matchesCategory = currentFilter.equals("All") || faq.getCategory().equals(currentFilter);
                    boolean matchesSearch = searchQuery.isEmpty() ||
                            faq.getQuestion().toLowerCase().contains(searchQuery) ||
                            faq.getAnswer().toLowerCase().contains(searchQuery);
                    return matchesCategory && matchesSearch;
                })
                .collect(Collectors.toList());

        adapter.setFilteredList(filteredFAQs);

        if (filteredFAQs.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupBackButton() {
        backButton.setOnClickListener(view -> {
            finish();
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
                startActivity(new Intent(Faqs.this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(Faqs.this, AttendanceStreak.class));
                return true;
            } else if (itemId == R.id.menu_attendance_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(Faqs.this, AttendanceHistoryActivity.class));
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(Faqs.this, ArchiveActivity.class));
                return true;
            } else if (itemId == R.id.menu_settings) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(Faqs.this, SettingsActivity.class));
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
                                startActivity(new Intent(Faqs.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(Faqs.this, TeacherHome.class));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
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
        Intent intent = new Intent(Faqs.this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}