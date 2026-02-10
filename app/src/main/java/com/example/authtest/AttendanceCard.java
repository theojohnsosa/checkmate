package com.example.authtest;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

// Reusable component card that displays attendance session controls for teachers
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

    /*
        Finds all UI references from root view
        Sets click listener on AttendanceButton that toggles isSessionActive
        Calls listener callbacks when session starts/ends
     */
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

    /*
        Stores reference to listener interface
        Allows parent activity to respond to session state changes
     */
    public void setOnAttendanceSessionListener(OnAttendanceSessionListener listener) {
        this.listener = listener;
    }

    /*
        Changes button text between "Start Attendance Session" and "End Attendance Session"
        Updates button and bell icon background colors:
        - Green (#2EAD00) = session inactive
        - Red (#FA5252) = session active
        Updates class code card background color accordingly
     */
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