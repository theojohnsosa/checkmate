package com.example.authtest;

import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import android.widget.LinearLayout;

public class SettingsActivity extends AppCompatActivity {

    private Switch appearanceToggle;
    private Switch notificationsToggle;
    private Switch doNotDisturbToggle;
    private LinearLayout appIconButton;
    private LinearLayout shareFeedbackButton;
    private LinearLayout termsOfServicesButton;
    private LinearLayout privacyPolicyButton;
    private LinearLayout faqsButton;
    private AppCompatButton logoutButton;
    private AppCompatButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        setupToggleListeners();
        setupButtonListeners();
        setupBackButton();
    }

    private void initializeViews() {
        appearanceToggle = findViewById(R.id.appearanceToggle);
        notificationsToggle = findViewById(R.id.notificationsToggle);
        doNotDisturbToggle = findViewById(R.id.doNotDisturbToggle);
        appIconButton = findViewById(R.id.appIconButton);
        shareFeedbackButton = findViewById(R.id.shareFeedbackButton);
        termsOfServicesButton = findViewById(R.id.termsOfServicesButton);
        privacyPolicyButton = findViewById(R.id.privacyPolicyButton);
        faqsButton = findViewById(R.id.faqsButton);
        logoutButton = findViewById(R.id.logoutButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupToggleListeners() {
        appearanceToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Toggle functionality will be added later
        });

        notificationsToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Toggle functionality will be added later
        });

        doNotDisturbToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Toggle functionality will be added later
        });
    }

    private void setupButtonListeners() {
        appIconButton.setOnClickListener(v ->
                Toast.makeText(this, "App Icon feature coming soon", Toast.LENGTH_SHORT).show()
        );

        shareFeedbackButton.setOnClickListener(v ->
                Toast.makeText(this, "Share Feedback feature coming soon", Toast.LENGTH_SHORT).show()
        );

        termsOfServicesButton.setOnClickListener(v ->
                Toast.makeText(this, "Terms of Services feature coming soon", Toast.LENGTH_SHORT).show()
        );

        privacyPolicyButton.setOnClickListener(v ->
                Toast.makeText(this, "Privacy Policy feature coming soon", Toast.LENGTH_SHORT).show()
        );

        faqsButton.setOnClickListener(v ->
                Toast.makeText(this, "FAQs feature coming soon", Toast.LENGTH_SHORT).show()
        );

        logoutButton.setOnClickListener(v ->
                Toast.makeText(this, "Logout feature coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupBackButton() {
        backButton.setOnClickListener(v -> finish());
    }
}