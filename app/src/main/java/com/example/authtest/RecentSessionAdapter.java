package com.example.authtest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecentSessionAdapter extends RecyclerView.Adapter<RecentSessionAdapter.SessionViewHolder> {

    private List<RecentSession> sessionList = new ArrayList<>();

    public RecentSessionAdapter() {
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recent_session_item, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        RecentSession session = sessionList.get(position);
        holder.bind(session);
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public void setSessions(List<RecentSession> sessions) {
        if (sessions == null) {
            this.sessionList = new ArrayList<>();
        } else {
            this.sessionList = sessions;
        }
        notifyDataSetChanged();
    }

    public void addSession(RecentSession session) {
        if (session != null) {
            this.sessionList.add(0, session); // Add to beginning to show newest first
            notifyItemInserted(0);
        }
    }

    class SessionViewHolder extends RecyclerView.ViewHolder {
        private ImageView calendarIcon;
        private TextView dateText;
        private TextView timeRangeText;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            calendarIcon = itemView.findViewById(R.id.calendarIcon);
            dateText = itemView.findViewById(R.id.dateText);
            timeRangeText = itemView.findViewById(R.id.timeRangeText);
        }

        public void bind(RecentSession session) {
            if (session == null) {
                return;
            }

            dateText.setText(session.getDate() != null ? session.getDate() : "N/A");
            timeRangeText.setText(session.getSessionTimeRange() != null ? session.getSessionTimeRange() : "N/A");
        }
    }
}