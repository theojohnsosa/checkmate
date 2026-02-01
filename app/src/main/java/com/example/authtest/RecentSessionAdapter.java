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
    private OnSessionClickListener clickListener;
    private OnSessionRemoveListener removeListener;

    public interface OnSessionClickListener {
        void onSessionClick(RecentSession session);
    }

    public interface OnSessionRemoveListener {
        void onSessionRemove(RecentSession session, int position);
    }

    public RecentSessionAdapter() {
    }

    public void setClickListener(OnSessionClickListener listener) {
        this.clickListener = listener;
    }

    public void setRemoveListener(OnSessionRemoveListener listener) {
        this.removeListener = listener;
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
        holder.bind(session, clickListener);
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public void setSessions(List<RecentSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            this.sessionList.clear();
        } else {
            this.sessionList = new ArrayList<>(sessions);

            for (int i = 0; i < this.sessionList.size(); i++) {
                RecentSession session = this.sessionList.get(i);
            }
        }

        notifyDataSetChanged();
    }

    public RecentSession getSessionAt(int position) {
        if (position >= 0 && position < sessionList.size()) {
            return sessionList.get(position);
        }
        return null;
    }

    public void triggerRemoval(RecentSession session, int position) {
        if (removeListener != null) {
            removeListener.onSessionRemove(session, position);
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

        public void bind(RecentSession session, OnSessionClickListener clickListener) {
            if (session == null) {
                return;
            }

            dateText.setText(session.getDate() != null ? session.getDate() : "N/A");
            timeRangeText.setText(session.getSessionTimeRange() != null ? session.getSessionTimeRange() : "N/A");

            itemView.setOnClickListener(view -> {
                if (clickListener != null) {
                    clickListener.onSessionClick(session);
                }
            });
        }
    }
}