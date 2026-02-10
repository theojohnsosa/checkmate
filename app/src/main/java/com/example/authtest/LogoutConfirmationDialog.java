package com.example.authtest;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;

// Confirmation dialog when user logs out
public class LogoutConfirmationDialog extends Dialog {

    private final Runnable onConfirm;
    private final Runnable onCancel;
    private AppCompatButton logoutButton;

    /*
         Takes onConfirm and onCancel runnables as parameters
         Displays message: "Are you sure you want to logout of your account?"
         Logout button calls onConfirm (which signs out and navigates to SignIn)
         Cancel button calls onCancel to close dialog
     */
    public LogoutConfirmationDialog(
            Context context,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        super(context);
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_logout);
        setCancelable(false);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView messageText = findViewById(R.id.logoutConfirmationMessage);
        logoutButton = findViewById(R.id.logoutButton);
        AppCompatButton cancelButton = findViewById(R.id.cancelButton);

        messageText.setText("Are you sure you want to logout of your account?");

        cancelButton.setOnClickListener(view -> {
            dismiss();
            if (onCancel != null) {
                onCancel.run();
            }
        });

        logoutButton.setOnClickListener(view -> {
            dismiss();
            if (onConfirm != null) {
                onConfirm.run();
            }
        });
    }
}