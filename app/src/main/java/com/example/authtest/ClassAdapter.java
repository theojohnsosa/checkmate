package com.example.authtest;

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

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    private List<ClassModel> classList = new ArrayList<>();
    private OnClassClickListener listener;

    public interface OnClassClickListener {
        void onClassClick(ClassModel classModel);
    }

    public ClassAdapter(OnClassClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_class_card, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        ClassModel classModel = classList.get(position);
        holder.bind(classModel, listener);
    }

    @Override
    public int getItemCount() {
        Log.d("ClassAdapter", "getItemCount() called - returning: " + classList.size());
        return classList.size();
    }

    public void setClasses(List<ClassModel> classes) {
        Log.d("ClassAdapter", "setClasses() called with " + (classes != null ? classes.size() : 0) + " items");
        this.classList = classes;
        notifyDataSetChanged();
        Log.d("ClassAdapter", "notifyDataSetChanged() called");
    }

    public ClassModel getClassAt(int position) {
        if (position >= 0 && position < classList.size()) {
            return classList.get(position);
        }
        return null;
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView classNameText;
        private TextView subjectCodeText;
        private TextView daysText;
        private TextView timeText;
        private TextView roomText;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.classCard);
            classNameText = itemView.findViewById(R.id.classNameText);
            subjectCodeText = itemView.findViewById(R.id.subjectCodeText);
            daysText = itemView.findViewById(R.id.daysText);
            timeText = itemView.findViewById(R.id.timeText);
            roomText = itemView.findViewById(R.id.roomText);
        }

        public void bind(ClassModel classModel, OnClassClickListener listener) {
            classNameText.setText(classModel.getClassName());
            subjectCodeText.setText(classModel.getSubjectCode());
            daysText.setText(classModel.getClassDays());

            String time = classModel.getStartTime() + " - " + classModel.getEndTime();
            timeText.setText(time);

            roomText.setText(classModel.getRoom());

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClassClick(classModel);
                }
            });
        }
    }
}