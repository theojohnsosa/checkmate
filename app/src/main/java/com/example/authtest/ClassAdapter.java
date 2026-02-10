package com.example.authtest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

// RecyclerView adapter for displaying list of classes
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

    /*
        Retrieves ClassModel at position
        Populates text views with: class name, subject code, meeting days, time range, room
        Formats time as "startTime - endTime" (e.g., "9:00 AM - 10:30 AM")
        Sets up click listener that invokes OnClassListener callback
     */
    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        ClassModel classModel = classList.get(position);
        holder.bind(classModel, listener);
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    /*
        Replaces internal classList with new data
        Calls notifyDataSetChanged() to trigger RecyclerView refresh
     */
    public void setClasses(List<ClassModel> classes) {
        this.classList = classes;
        notifyDataSetChanged();
    }

    /*
        Returns class model at given position
        Checks bounds to prevent IndexOutOfBoundsException
        Returns null if position is invalid
     */
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

            cardView.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onClassClick(classModel);
                }
            });
        }
    }
}