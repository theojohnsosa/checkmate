package com.example.authtest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryAdapter extends RecyclerView.Adapter<AttendanceHistoryAdapter.HistoryViewHolder> {

    private List<AttendanceHistoryModel> historyList;

    public AttendanceHistoryAdapter(List<AttendanceHistoryModel> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.attendance_history_item_card, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        AttendanceHistoryModel history = historyList.get(position);
        holder.bind(history);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void setHistory(List<AttendanceHistoryModel> newHistory) {
        this.historyList = newHistory;
        notifyDataSetChanged();
    }

    public void updateList(List<AttendanceHistoryModel> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView classNameText;
        private TextView classCodeText;
        private TextView subjectCodeText;
        private TextView dateAddedText;
        private TextView timeText;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            classNameText = itemView.findViewById(R.id.historyClassName);
            classCodeText = itemView.findViewById(R.id.historyClassCode);
            subjectCodeText = itemView.findViewById(R.id.historySubjectCode);
            dateAddedText = itemView.findViewById(R.id.historyDateAdded);
            timeText = itemView.findViewById(R.id.historyTime);
        }

        public void bind(AttendanceHistoryModel history) {
            classNameText.setText(history.getClassName());
            classCodeText.setText(history.getClassCode());
            subjectCodeText.setText(history.getSubjectCode());

            String timeRange = history.getStartTime() + " - " + history.getEndTime();
            timeText.setText(timeRange);

            String formattedDate = formatDate(history.getDateAdded());
            dateAddedText.setText(formattedDate);
        }

        private String formatDate(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}