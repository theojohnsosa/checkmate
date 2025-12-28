package com.example.authtest;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StudentHome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.joinClassButton).setOnClickListener(v -> showJoinClassDialog());

        findViewById(R.id.ctaButton).setOnClickListener(v -> showJoinClassDialog());

    }

    private void showJoinClassDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_join_class);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();

            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());

            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;

            window.setAttributes(params);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.6f);
        }

        EditText classCodeInput = dialog.findViewById(R.id.classCodeInput);
        AppCompatButton cancelButton = dialog.findViewById(R.id.cancelButton);
        AppCompatButton joinButton = dialog.findViewById(R.id.joinButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        joinButton.setOnClickListener(v -> {
            String classCode = classCodeInput.getText().toString().trim();

            if (classCode.isEmpty()) {
                classCodeInput.setError("Class code is required");
                return;
            }

            // TODO: Join class logic (Firestore lookup, etc.)
            dialog.dismiss();
        });

        dialog.show();
    }

}