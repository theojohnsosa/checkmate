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
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class StudentSeatDialog extends Dialog {

    public interface OnStatusChangedListener {
        void onStatusChanged();
    }

    private final StudentAttendanceModel student;
    private final String classId;
    private final OnStatusChangedListener statusChangedListener;
    private final boolean isTeacher;

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
            OnStatusChangedListener listener,
            boolean isTeacher
    ) {
        super(context);
        this.student = student;
        this.classId = classId;
        this.statusChangedListener = listener;
        this.isTeacher = isTeacher;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.seatplan_dialog);
        setCancelable(false);

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
        attendanceStatusText = findViewById(R.id.attendanceStatusText);
        statusBadge = findViewById(R.id.statusBadge);
        AppCompatButton closeButton = findViewById(R.id.closeButton1);
        falseButton = findViewById(R.id.falseButton);
        undoButton = findViewById(R.id.undoButton);

        studentNameText.setText(student.getFullName());
        studentEmailText.setText(student.getEmail());

        previousStatus = student.getAttendanceStatus();

        if (previousStatus == null || previousStatus.trim().isEmpty()) {
            previousStatus = "Not Marked";
        }

        updateStatusUI(previousStatus);

        if (!isTeacher) {
            if (falseButton != null) falseButton.setVisibility(View.GONE);
            if (undoButton != null) undoButton.setVisibility(View.GONE);
        } else {
            checkIfAlreadyFalseMarked();
        }

        if (closeButton != null) closeButton.setOnClickListener(view -> closeDialog());
        if (falseButton != null && isTeacher) falseButton.setOnClickListener(view -> markAsFalse());
        if (undoButton != null && isTeacher) undoButton.setOnClickListener(view -> undoFalseMarking());
    }

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

    private void updateStatusUI(String status) {
        attendanceStatusText.setText(status);

        int bgColor = 0xFF2C2C2C;
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

    private void markAsFalse() {
        String studentId = student.getStudentId();
        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Student ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .document(studentId)
                .update(
                        "falseMarked", true,
                        "originalStatus", previousStatus
                )
                .addOnSuccessListener(unused -> {
                    // Mark false in attendance collection + remove streak/leaderboard data
                    removeStreakAndLeaderboardData(db, studentId);

                    incrementViolations(db, studentId);

                    isFalseMarked = true;
                    student.setAttendanceStatus("False");
                    updateStatusUI("False");
                    showUndoButton();

                    if (statusChangedListener != null) statusChangedListener.onStatusChanged();
                    Toast.makeText(getContext(), "Marked as False", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(unused -> {
                    Toast.makeText(getContext(), "Cannot mark false yet", Toast.LENGTH_SHORT).show();
                });
    }

    private void undoFalseMarking() {
        if (!isFalseMarked) return;

        String studentId = student.getStudentId();
        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Student ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // First, get the original timestamp from attendanceRecords so we can restore properly
        db.collection("allClasses")
                .document(classId)
                .collection("attendanceRecords")
                .document(studentId)
                .get()
                .addOnSuccessListener(doc -> {
                    Long originalTimestamp = doc.getLong("timestamp");

                    doc.getReference()
                            .update(
                                    "falseMarked", false,
                                    "originalStatus", null
                            )
                            .addOnSuccessListener(unused -> {
                                // Restore streak and leaderboard data if the original status was "Present"
                                if ("Present".equals(previousStatus) && originalTimestamp != null) {
                                    restoreStreakAndLeaderboardData(db, studentId, originalTimestamp);
                                } else {
                                    // Just unflag falseMarked in the attendance collection
                                    unflagAttendanceCollection(db, studentId);
                                }

                                decrementViolations(db, studentId);

                                isFalseMarked = false;
                                student.setAttendanceStatus(previousStatus);
                                updateStatusUI(previousStatus);
                                hideUndoButton();

                                if (statusChangedListener != null) statusChangedListener.onStatusChanged();
                                Toast.makeText(getContext(), "Undo successful", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to undo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to undo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * When marking as False:
     * - Find the attendance document for this student/class/today and set falseMarked = true
     *   This makes AttendanceStreak skip it (streak goes dark) and Leaderboards skip it (point removed).
     */
    private void removeStreakAndLeaderboardData(FirebaseFirestore db, String studentId) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String today = dateFormat.format(new java.util.Date());

        db.collection("attendance")
                .whereEqualTo("userId", studentId)
                .whereEqualTo("classId", classId)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // Setting falseMarked = true causes both AttendanceStreak and Leaderboards
                        // to skip this record — streak card goes dark, point is no longer counted.
                        doc.getReference().update("falseMarked", true)
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(),
                                                "Failed to update attendance record: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to find attendance record: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * When undoing False on a "Present" student:
     * - If an attendance doc already exists with falseMarked=true → set falseMarked=false to restore streak & leaderboard point.
     * - If no attendance doc exists (it was never saved, e.g. marking happened before saveAttendanceForStreak ran)
     *   → create one now so the streak and leaderboard are both restored.
     */
    private void restoreStreakAndLeaderboardData(FirebaseFirestore db, String studentId, long originalTimestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String today = dateFormat.format(new java.util.Date(originalTimestamp));

        db.collection("attendance")
                .whereEqualTo("userId", studentId)
                .whereEqualTo("classId", classId)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Document exists — just clear the falseMarked flag
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            doc.getReference().update("falseMarked", false)
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(),
                                                    "Failed to restore attendance record: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show()
                                    );
                        }
                    } else {
                        // No document found — create it so the streak and leaderboard are restored
                        Map<String, Object> streakAttendance = new HashMap<>();
                        streakAttendance.put("userId", studentId);
                        streakAttendance.put("date", today);
                        streakAttendance.put("status", "present");
                        streakAttendance.put("timestamp", originalTimestamp);
                        streakAttendance.put("classId", classId);
                        streakAttendance.put("falseMarked", false);

                        db.collection("attendance")
                                .add(streakAttendance)
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(),
                                                "Failed to restore attendance record: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to restore attendance record: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * For non-Present statuses on undo: just clear the falseMarked flag in attendance collection
     * (no streak/leaderboard impact since only "present" records count).
     */
    private void unflagAttendanceCollection(FirebaseFirestore db, String studentId) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String today = dateFormat.format(new java.util.Date());

        db.collection("attendance")
                .whereEqualTo("userId", studentId)
                .whereEqualTo("classId", classId)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update("falseMarked", false);
                    }
                });
    }

    private void incrementViolations(FirebaseFirestore db, String studentId) {
        db.collection("users")
                .document(studentId)
                .update("violations", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Violations updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    db.collection("users")
                            .document(studentId)
                            .set(new java.util.HashMap<String, Object>() {{
                                put("violations", 1);
                            }}, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Violations field created", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void decrementViolations(FirebaseFirestore db, String studentId) {
        db.collection("users")
                .document(studentId)
                .update("violations", FieldValue.increment(-1))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Violations updated", Toast.LENGTH_SHORT).show();
                });
    }

    private void showUndoButton() {
        if (undoButton != null) undoButton.setVisibility(View.VISIBLE);
        if (falseButton != null) falseButton.setVisibility(View.GONE);
    }

    private void hideUndoButton() {
        if (undoButton != null) undoButton.setVisibility(View.GONE);
        if (falseButton != null) falseButton.setVisibility(View.VISIBLE);
    }

    public void closeDialog() {
        if (isShowing()) dismiss();
    }
}