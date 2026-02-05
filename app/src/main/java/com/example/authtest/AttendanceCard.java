package com.example.authtest;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

public class AttendanceCard {

    private TextView bellIcon;
    private CardView classCodeCard;
    private AppCompatButton attendanceButton;
    private TextView classCodeText;
    private boolean isSessionActive = false;
    private OnAttendanceSessionListener listener;

    public interface OnAttendanceSessionListener {
        void onSessionStarted();
        void onSessionEnded();
    }

    public void setup(View rootView) {
        bellIcon = rootView.findViewById(R.id.bellIcon);
        classCodeCard = rootView.findViewById(R.id.classCodeCard);
        attendanceButton = rootView.findViewById(R.id.attendanceButton);
        classCodeText = rootView.findViewById(R.id.classCodeText);

        if (attendanceButton != null) {
            attendanceButton.setOnClickListener(view -> {
                isSessionActive = !isSessionActive;
                updateState();

                if (listener != null) {
                    if (isSessionActive) {
                        listener.onSessionStarted();
                    } else {
                        listener.onSessionEnded();
                    }
                }
            });
        }

        updateState();
    }

    public void setClassCode(String classCode) {
        if (classCodeText != null) {
            classCodeText.setText(classCode);
        }
    }

    public void setOnAttendanceSessionListener(OnAttendanceSessionListener listener) {
        this.listener = listener;
    }

    private void updateState() {
        if (attendanceButton == null || bellIcon == null || classCodeCard == null) {
            return;
        }

        if (isSessionActive) {
            attendanceButton.setText("End Attendance Session");
            attendanceButton.setBackgroundResource(R.drawable.alt_attendance_button);
            bellIcon.setBackgroundResource(R.drawable.alt_attendance_button);
            classCodeCard.setCardBackgroundColor(Color.parseColor("#FA5252"));
        } else {
            attendanceButton.setText("Start Attendance Session");
            attendanceButton.setBackgroundResource(R.drawable.attendance_button);
            bellIcon.setBackgroundResource(R.drawable.attendance_button);
            classCodeCard.setCardBackgroundColor(Color.parseColor("#2EAD00"));
        }
    }
}