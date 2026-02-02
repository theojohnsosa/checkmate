package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;

public class ShareFeedbackActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private AppCompatButton backButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AppCompatButton buttonSlow, buttonConfusing, buttonFast, buttonFrustrating;
    private AppCompatButton buttonConvenient, buttonEasyToUse, buttonReliable, buttonTimeSaving;
    private AppCompatButton buttonNeedsImprovement, buttonGreatDesign, buttonIntuitive, buttonHelpful;
    private ImageView star1, star2, star3, star4, star5;
    private int selectedStarRating = 0;
    private LinearLayout recommendationNo, recommendationYes;
    private int selectedRecommendation = 0;
    private EditText feedbackTextInput;
    private AppCompatButton submitFeedbackButton;
    private Set<AppCompatButton> selectedFeelingButtons = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_feedback);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.main);
        navigationView = findViewById(R.id.navigation_view);

        ImageView hamburgerIcon = findViewById(R.id.hamburger_icon);
        if (hamburgerIcon != null) {
            hamburgerIcon.setOnClickListener(view -> {
                drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        setupNavigationDrawer();
        loadUserInfoInDrawer();
        setupBackPressHandler();
        initializeViews();
        setupFeelingButtonListeners();
        setupStarRatingListeners();
        setupRecommendationListeners();
        setupSubmitButton();
        setupBackButton();
        checkUserTypeAndConfigureMenu();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);

        buttonSlow = findViewById(R.id.buttonSlow);
        buttonConfusing = findViewById(R.id.buttonConfusing);
        buttonFast = findViewById(R.id.buttonFast);
        buttonFrustrating = findViewById(R.id.buttonFrustrating);
        buttonConvenient = findViewById(R.id.buttonConvenient);
        buttonEasyToUse = findViewById(R.id.buttonEasyToUse);
        buttonReliable = findViewById(R.id.buttonReliable);
        buttonTimeSaving = findViewById(R.id.buttonTimeSaving);
        buttonNeedsImprovement = findViewById(R.id.buttonNeedsImprovement);
        buttonGreatDesign = findViewById(R.id.buttonGreatDesign);
        buttonIntuitive = findViewById(R.id.buttonIntuitive);
        buttonHelpful = findViewById(R.id.buttonHelpful);

        star1 = findViewById(R.id.star1);
        star2 = findViewById(R.id.star2);
        star3 = findViewById(R.id.star3);
        star4 = findViewById(R.id.star4);
        star5 = findViewById(R.id.star5);

        recommendationNo = findViewById(R.id.recommendationNo);
        recommendationYes = findViewById(R.id.recommendationYes);

        feedbackTextInput = findViewById(R.id.feedbackTextInput);
        submitFeedbackButton = findViewById(R.id.submitFeedbackButton);
    }

    private void setupFeelingButtonListeners() {
        AppCompatButton[] buttons = {
                buttonSlow,
                buttonConfusing,
                buttonFast,
                buttonFrustrating,
                buttonConvenient,
                buttonEasyToUse,
                buttonReliable,
                buttonTimeSaving,
                buttonNeedsImprovement,
                buttonGreatDesign,
                buttonIntuitive,
                buttonHelpful
        };

        for (AppCompatButton button : buttons) {
            if (button != null) {
                button.setOnClickListener(view -> {
                    toggleFeelingButton(button);
                });
            }
        }
    }

    private void toggleFeelingButton(AppCompatButton button) {
        if (selectedFeelingButtons.contains(button)) {
            selectedFeelingButtons.remove(button);
            button.setTextColor(0xFFFFFFFF);
            button.setBackgroundResource(R.drawable.feedback_button_unselected);
        } else {
            selectedFeelingButtons.add(button);
            button.setTextColor(0xFF000000);
            button.setBackgroundResource(R.drawable.feedback_button_selected);
        }
    }

    private void setupStarRatingListeners() {
        ImageView[] stars = {star1, star2, star3, star4, star5};

        for (int i = 0; i < stars.length; i++) {
            if (stars[i] != null) {
                final int rating = i + 1;
                stars[i].setOnClickListener(view -> {
                    selectStarRating(rating);
                });
            }
        }
    }

    private void selectStarRating(int rating) {
        selectedStarRating = rating;
        ImageView[] stars = {star1, star2, star3, star4, star5};

        for (int i = 0; i < stars.length; i++) {
            if (stars[i] != null) {
                if (i < rating) {
                    stars[i].setImageResource(R.drawable.ic_star_yellow);
                } else {
                    stars[i].setImageResource(R.drawable.ic_star_gray);
                }
            }
        }
    }

    private void setupRecommendationListeners() {
        if (recommendationNo != null) {
            recommendationNo.setOnClickListener(view -> {
                selectRecommendation(1);
            });
        }
        if (recommendationYes != null) {
            recommendationYes.setOnClickListener(view -> {
                selectRecommendation(2);
            });
        }
    }

    private void selectRecommendation(int recommendation) {
        selectedRecommendation = recommendation;

        recommendationNo.setBackgroundResource(R.drawable.feedback_button_unselected);
        recommendationYes.setBackgroundResource(R.drawable.feedback_button_unselected);

        ImageView thumbsDownNo = (ImageView) recommendationNo.getChildAt(0);
        ImageView thumbsUpYes = (ImageView) recommendationYes.getChildAt(0);

        TextView textNo = (TextView) recommendationNo.getChildAt(1);
        TextView textYes = (TextView) recommendationYes.getChildAt(1);

        if (thumbsDownNo != null) {
            thumbsDownNo.setImageResource(R.drawable.ic_thumbs_down_white);
        }
        if (thumbsUpYes != null) {
            thumbsUpYes.setImageResource(R.drawable.ic_thumbs_up_white);
        }

        if (textNo != null) {
            textNo.setTextColor(0xFFFFFFFF);
        }
        if (textYes != null) {
            textYes.setTextColor(0xFFFFFFFF);
        }

        if (recommendation == 1) {
            recommendationNo.setBackgroundResource(R.drawable.feedback_button_selected);
            if (thumbsDownNo != null) {
                thumbsDownNo.setImageResource(R.drawable.ic_thumbs_down_gray);
            }
            if (textNo != null) {
                textNo.setTextColor(0xFF000000);
            }
        } else if (recommendation == 2) {
            recommendationYes.setBackgroundResource(R.drawable.feedback_button_selected);
            if (thumbsUpYes != null) {
                thumbsUpYes.setImageResource(R.drawable.ic_thumbs_up_gray);
            }
            if (textYes != null) {
                textYes.setTextColor(0xFF000000);
            }
        }
    }

    private void setupSubmitButton() {
        if (submitFeedbackButton != null) {
            submitFeedbackButton.setOnClickListener(view -> {
                if (validateForm()) {
                    submitFeedback();
                }
            });
        }
    }

    private boolean validateForm() {
        if (selectedFeelingButtons.isEmpty()) {
            Toast.makeText(this, "Please select at least one feeling", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedStarRating == 0) {
            Toast.makeText(this, "Please rate our service", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedRecommendation == 0) {
            Toast.makeText(this, "Please select whether you would recommend Checkmate", Toast.LENGTH_SHORT).show();
            return false;
        }

        String feedbackText = feedbackTextInput.getText().toString().trim();
        if (feedbackText.isEmpty()) {
            Toast.makeText(this, "Please provide feedback text", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void submitFeedback() {
        Toast.makeText(this, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setupBackButton() {
        if (backButton != null) {
            backButton.setOnClickListener(view -> {
                finish();
            });
        }
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
                startActivity(new Intent(ShareFeedbackActivity.this, ProfilePage.class));
                return true;
            } else if (itemId == R.id.menu_streak) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(ShareFeedbackActivity.this, AttendanceStreak.class));
                return true;
            } else if (itemId == R.id.menu_attendance_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(ShareFeedbackActivity.this, AttendanceHistoryActivity.class));
                return true;
            } else if (itemId == R.id.menu_archive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(ShareFeedbackActivity.this, ArchiveActivity.class));
                return true;
            } else if (itemId == R.id.menu_settings) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(ShareFeedbackActivity.this, SettingsActivity.class));
                return true;
            } else if (itemId == R.id.menu_logout) {
                LogoutConfirmationDialog confirmDialog = new LogoutConfirmationDialog(ShareFeedbackActivity.this, this::logout,
                        () -> drawerLayout.closeDrawer(GravityCompat.START)
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
                                startActivity(new Intent(ShareFeedbackActivity.this, StudentHome.class));
                            } else if ("Teacher".equalsIgnoreCase(userType.trim())) {
                                startActivity(new Intent(ShareFeedbackActivity.this, TeacherHome.class));
                            } else {
                                Toast.makeText(ShareFeedbackActivity.this, "Unknown user type: " + userType, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ShareFeedbackActivity.this, "User type not found in document", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ShareFeedbackActivity.this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    Toast.makeText(ShareFeedbackActivity.this, "Error loading user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

                            if (userNameTextView != null) {
                                userNameTextView.setText(fullName.isEmpty() ? "User" : fullName);
                            }
                        } else if (userNameTextView != null) {
                            userNameTextView.setText("User");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (userNameTextView != null) {
                            userNameTextView.setText("User");
                        }
                    });

            if (userEmailTextView != null) {
                userEmailTextView.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
            }
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
        Intent intent = new Intent(ShareFeedbackActivity.this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}