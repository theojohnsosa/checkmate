package com.example.authtest;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

public class StudentSeatDialog extends Dialog {

    private final StudentAttendanceModel student;

    public StudentSeatDialog(
            Context context,

            StudentAttendanceModel student) {
        super(context);
        this.student = student;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.seatplan_dialog);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.92),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView studentNameText = findViewById(R.id.studentNameText);
        TextView studentEmailText = findViewById(R.id.studentEmailText);
        TextView attendanceStatusText = findViewById(R.id.attendanceStatusText);
        CardView statusBadge = findViewById(R.id.statusBadge);
        AppCompatButton closeButton = findViewById(R.id.closeButton1);

        // --- BASIC DATA ---
        studentNameText.setText(student.getFullName());
        studentEmailText.setText(student.getEmail());

        // status
        String status = student.getAttendanceStatus();

        if (status == null || status.trim().isEmpty()) {
            status = "Not Marked";
        }

        attendanceStatusText.setText(status);

        //reset the color
        statusBadge.setCardBackgroundColor(0xFF2C2C2C);
        attendanceStatusText.setTextColor(0xFF828282);

        // update status
        switch (status) {
            case "Present":
                statusBadge.setCardBackgroundColor(0xFF51CF66);
                attendanceStatusText.setTextColor(0xFFFFFFFF);
                break;

            case "Late":
                statusBadge.setCardBackgroundColor(0xFFFFA94D);
                attendanceStatusText.setTextColor(0xFFFFFFFF);
                break;

            case "Absent":
                statusBadge.setCardBackgroundColor(0xFFFF6B6B);
                attendanceStatusText.setTextColor(0xFFFFFFFF);
                break;

            default:
                // "Not Marked" → keep default styling
                break;
        }

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> closeDialog());
        }
        setCanceledOnTouchOutside(true);

    }
    public void closeDialog() {
        if (isShowing()) {
            dismiss();
        }
    }
}

