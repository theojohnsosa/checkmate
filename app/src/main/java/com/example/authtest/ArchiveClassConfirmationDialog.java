package com.example.authtest;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;

// Confirmation dialog when teacher archives a class
public class ArchiveClassConfirmationDialog extends Dialog {

    private final Runnable onConfirm;
    private final Runnable onCancel;
    private AppCompatButton archiveButton;

    /*
        Takes className parameter to personalize the confirmation message
        Sets up transparent dialog with custom layout
        Displays message: "Are you sure you want to archive [className]?"
        Has Cancel and Archive Buttons
     */
    public ArchiveClassConfirmationDialog(
            Context context,
            String className,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        super(context);
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_archive_class);
        setCancelable(false);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView messageText = findViewById(R.id.messageText);
        archiveButton = findViewById(R.id.archiveButton);
        AppCompatButton cancelButton = findViewById(R.id.cancelButton);

        messageText.setText("Are you sure you want to archive \"" + className + "\"?");

        // Runnable pattern allows parent activity to handle the archive action
        // Cancel button: dismisses dialog and calls onCancel runnable
        cancelButton.setOnClickListener(view -> {
            dismiss();
            if (onCancel != null) {
                onCancel.run();
            }
        });

        // Archive button: dismisses dialog and calls onConfirm runnable
        archiveButton.setOnClickListener(view -> {
            dismiss();
            if (onConfirm != null) {
                onConfirm.run();
            }
        });

        setOnCancelListener(dialog -> {
            if (onCancel != null) {
                onCancel.run();
            }
        });
    }
}