package com.example.authtest;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;

public class StudentSeatDialog extends Dialog {


    public interface OnStatusChangedListener {
        void onStatusChanged();
    }

    // Fields
    private final StudentAttendanceModel student;
    private final String classId;
    private final OnStatusChangedListener statusChangedListener;

    private AppCompatButton falseButton;
    private AppCompatButton undoButton;
    private String previousStatus;
    private boolean isFalseMarked = false;

    private TextView attendanceStatusText;
    private CardView statusBadge;

    public StudentSeatDialog(
            Context context,
            StudentAttendanceModel student,
            String classId,
            OnStatusChangedListener listener
    ) {
        super(context);
        this.student = student;
        this.classId = classId;
        this.statusChangedListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.seatplan_dialog);
        setCancelable(false);

        // Dialog window styling
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.92),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // UI elements
        TextView studentNameText = findViewById(R.id.studentNameText);
        TextView studentEmailText = findViewById(R.id.studentEmailText);
        attendanceStatusText = findViewById(R.id.attendanceStatusText);
        statusBadge = findViewById(R.id.statusBadge);
        AppCompatButton closeButton = findViewById(R.id.closeButton1);
        falseButton = findViewById(R.id.falseButton);
        undoButton = findViewById(R.id.undoButton);

        // Set student info
        studentNameText.setText(student.getFullName());
        studentEmailText.setText(student.getEmail());

        // Initialize previous status
        previousStatus = student.getAttendanceStatus();
        if (previousStatus == null || previousStatus.trim().isEmpty()) {
            previousStatus = "Not Marked";
        }

        // Display initial status
        updateStatusUI(previousStatus);

        // Load whether student was already False-marked in Firestore
        checkIfAlreadyFalseMarked();

        // Close button
        if (closeButton != null) {
            closeButton.setOnClickListener(view -> closeDialog());
        }

        // False button
        if (falseButton != null) {
            falseButton.setOnClickListener(view -> markAsFalse());
        }

        // Undo button
        if (undoButton != null) {
            undoButton.setOnClickListener(view -> undoFalseMarking());
        }
    }

    // Check Firestore if student was already marked False
    private void checkIfAlreadyFalseMarked() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .document(student.getStudentId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean falseMarked = doc.getBoolean("falseMarked");
                        if (falseMarked != null && falseMarked) {
                            isFalseMarked = true;
                            String originalStatus = doc.getString("originalStatus");
                            if (originalStatus != null) previousStatus = originalStatus;
                            student.setAttendanceStatus("False");
                            updateStatusUI("False");
                            showUndoButton();
                        }
                    }
                });
    }

    // Update UI colors and text
    private void updateStatusUI(String status) {
        attendanceStatusText.setText(status);

        int bgColor = 0xFF2C2C2C; // default
        int textColor = 0xFF828282;

        switch (status) {
            case "Present":
                bgColor = 0xFF51CF66;
                textColor = 0xFFFFFFFF;
                break;
            case "Late":
                bgColor = 0xFFFFA94D;
                textColor = 0xFFFFFFFF;
                break;
            case "Absent":
                bgColor = 0xFFFF6B6B;
                textColor = 0xFFFFFFFF;
                break;
            case "False":
                bgColor = 0xFF004FB9;
                textColor = 0xFFFFFFFF;
                break;
        }

        statusBadge.setCardBackgroundColor(bgColor);
        attendanceStatusText.setTextColor(textColor);

    }

    // Mark student as False in Firestore
    private void markAsFalse() {
        if (isFalseMarked) {
            Toast.makeText(getContext(), "Already marked as False", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .document(student.getStudentId())
                .update(
                        "falseMarked", true,
                        "originalStatus", previousStatus
                )
                .addOnSuccessListener(unused -> {
                    isFalseMarked = true;
                    student.setAttendanceStatus("False");
                    updateStatusUI("False");
                    showUndoButton();

                    if (statusChangedListener != null) {
                        statusChangedListener.onStatusChanged();
                    }
                    Toast.makeText(getContext(), "Marked as False", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to mark as False: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Undo False marking
    private void undoFalseMarking() {
        if (!isFalseMarked) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .document(student.getStudentId())
                .update(
                        "falseMarked", false,
                        "originalStatus", null
                )
                .addOnSuccessListener(unused -> {
                    isFalseMarked = false;
                    student.setAttendanceStatus(previousStatus);
                    updateStatusUI(previousStatus);
                    hideUndoButton();
                    Toast.makeText(getContext(), "Undo successful", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to undo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Show Undo button and hide False button
    private void showUndoButton() {
        if (undoButton != null) undoButton.setVisibility(View.VISIBLE);
        if (falseButton != null) falseButton.setVisibility(View.GONE);
    }

    // Hide Undo button and show False button
    private void hideUndoButton() {
        if (undoButton != null) undoButton.setVisibility(View.GONE);
        if (falseButton != null) falseButton.setVisibility(View.VISIBLE);
    }

    // Close dialog
    public void closeDialog() {
        if (isShowing()) dismiss();
    }
}
