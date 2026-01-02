package com.example.authtest;

import android.graphics.Color;
import android.util.Log;
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

    private static final String TAG = "StudentAdapter";
    private List<StudentAttendanceModel> studentList = new ArrayList<>();
    private OnStudentRemoveListener removeListener;

    public interface OnStudentRemoveListener {
        void onStudentRemove(StudentAttendanceModel student, int position);
    }

    public StudentAttendanceAdapter(OnStudentRemoveListener removeListener) {
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
            Log.w(TAG, "Attempted to set null student list");
            this.studentList = new ArrayList<>();
        } else {
            this.studentList = students;
            Log.d(TAG, "Updated adapter with " + students.size() + " students");
        }
        notifyDataSetChanged();
    }

    public void removeStudent(int position) {
        if (position >= 0 && position < studentList.size()) {
            studentList.remove(position);
            notifyItemRemoved(position);
            Log.d(TAG, "Removed student at position " + position);
        }
    }

    public StudentAttendanceModel getStudentAt(int position) {
        if (position >= 0 && position < studentList.size()) {
            return studentList.get(position);
        }
        return null;
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
                Log.e(TAG, "Attempted to bind null student");
                return;
            }

            // Set student name with null safety
            String fullName = student.getFullName();
            if (fullName == null || fullName.trim().isEmpty()) {
                fullName = "Unknown User";
            }
            studentNameText.setText(fullName);

            // Set student email with null safety
            String email = student.getEmail();
            if (email == null || email.trim().isEmpty()) {
                email = "No email";
            }
            studentEmailText.setText(email);

            // Set attendance status with null safety
            String status = student.getAttendanceStatus();
            if (status == null || status.trim().isEmpty()) {
                status = "Not Marked";
            }
            attendanceStatusText.setText(status);

            // Log binding for debugging
            Log.d(TAG, "Binding: " + fullName + " (" + email + ") - Status: " + status);

            // Set status badge color based on attendance status
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