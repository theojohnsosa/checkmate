package com.example.authtest;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;

// Confirmation dialog when removing student from class
public class RemoveStudentConfirmationDialog extends Dialog {

    private final Runnable onConfirm;
    private final Runnable onCancel;
    private AppCompatButton removeButton;

    /*
         Personalizes message: "Are you sure you want to remove [studentName] from this class?"
         Allows teacher to confirm student removal
         Cancel preserves student enrollment
     */
    public RemoveStudentConfirmationDialog(
            Context context,
            String studentName,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        super(context);
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_remove_student);
        setCancelable(false);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView messageText = findViewById(R.id.removeConfirmationMessage);
        removeButton = findViewById(R.id.removeButton);
        AppCompatButton cancelButton = findViewById(R.id.cancelButton);

        messageText.setText("Are you sure you want to remove " + studentName + " from this class?");

        cancelButton.setOnClickListener(view -> {
            dismiss();
            if (onCancel != null) {
                onCancel.run();
            }
        });

        removeButton.setOnClickListener(view -> {
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