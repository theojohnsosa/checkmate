package com.example.authtest;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StudentAttendanceAdapter extends RecyclerView.Adapter<StudentAttendanceAdapter.StudentViewHolder> {

    private List<StudentAttendanceModel> studentList = new ArrayList<>();
    private OnStudentRemoveListener removeListener;
    private String sessionId;  // FIX #4: Add session context awareness

    public interface OnStudentRemoveListener {
        void onStudentRemove(StudentAttendanceModel student, int position);
    }

    // FIX #4: Updated constructor to accept sessionId
    public StudentAttendanceAdapter(String sessionId, OnStudentRemoveListener removeListener) {
        this.sessionId = sessionId;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.student_attendance_item, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentAttendanceModel student = studentList.get(position);
        holder.bind(student);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public void setStudents(List<StudentAttendanceModel> students) {
        if (students == null) {
            this.studentList = new ArrayList<>();
        } else {
            this.studentList = students;
        }
        notifyDataSetChanged();
    }

    public void removeStudent(int position) {
        if (position >= 0 && position < studentList.size()) {
            studentList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public StudentAttendanceModel getStudentAt(int position) {
        if (position >= 0 && position < studentList.size()) {
            return studentList.get(position);
        }
        return null;
    }

    // FIX #4: Add getter for session context verification
    public String getSessionId() {
        return sessionId;
    }

    class StudentViewHolder extends RecyclerView.ViewHolder {
        private TextView studentNameText;
        private TextView studentEmailText;
        private TextView attendanceStatusText;
        private CardView statusBadge;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameText = itemView.findViewById(R.id.studentNameText);
            studentEmailText = itemView.findViewById(R.id.studentEmailText);
            attendanceStatusText = itemView.findViewById(R.id.attendanceStatusText);
            statusBadge = itemView.findViewById(R.id.statusBadge);
        }

        public void bind(StudentAttendanceModel student) {
            if (student == null) {
                return;
            }

            String fullName = student.getFullName();
            if (fullName == null || fullName.trim().isEmpty()) {
                fullName = "Unknown User";
            }
            studentNameText.setText(fullName);

            String email = student.getEmail();
            if (email == null || email.trim().isEmpty()) {
                email = "No email";
            }
            studentEmailText.setText(email);

            String status = student.getAttendanceStatus();
            if (status == null || status.trim().isEmpty()) {
                status = "Not Marked";
            }
            attendanceStatusText.setText(status);

            switch (status) {
                case "Present":
                    statusBadge.setCardBackgroundColor(Color.parseColor("#2F9E44"));
                    attendanceStatusText.setTextColor(Color.parseColor("#FFFFFF"));
                    break;
                case "Late":
                    statusBadge.setCardBackgroundColor(Color.parseColor("#F59F00"));
                    attendanceStatusText.setTextColor(Color.parseColor("#FFFFFF"));
                    break;
                case "Absent":
                    statusBadge.setCardBackgroundColor(Color.parseColor("#C92A2A"));
                    attendanceStatusText.setTextColor(Color.parseColor("#FFFFFF"));
                    break;
                case "Not Marked":
                default:
                    statusBadge.setCardBackgroundColor(Color.parseColor("#2C2C2C"));
                    attendanceStatusText.setTextColor(Color.parseColor("#828282"));
                    break;
            }
        }
    }
}