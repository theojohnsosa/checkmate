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
        this.studentList = students;
        notifyDataSetChanged();
    }

    public void removeStudent(int position) {
        if (position >= 0 && position < studentList.size()) {
            studentList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public StudentAttendanceModel getStudentAt(int position) {
        return studentList.get(position);
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
            studentNameText.setText(student.getFullName());
            studentEmailText.setText(student.getEmail());
            attendanceStatusText.setText(student.getAttendanceStatus());

            // Set status badge color based on attendance status
            switch (student.getAttendanceStatus()) {
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