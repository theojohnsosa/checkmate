package com.example.authtest;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;

public class ArchiveClassConfirmationDialog extends Dialog {

    private final Runnable onConfirm;
    private final Runnable onCancel;
    private AppCompatButton archiveButton;

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

        cancelButton.setOnClickListener(view -> {
            dismiss();
            if (onCancel != null) {
                onCancel.run();
            }
        });

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