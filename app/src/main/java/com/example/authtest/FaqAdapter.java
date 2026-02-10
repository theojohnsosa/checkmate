package com.example.authtest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// RecyclerView adapter for FAQ expandable items
public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.FAQViewHolder> {

    private List<FaqItem> faqList;
    private int expandedPosition = -1;
    public FaqAdapter(List<FaqItem> faqList) {
        this.faqList = faqList;
    }

    @Override
    public FAQViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.faq_item, parent, false);
        return new FAQViewHolder(view);
    }

    /*
         Determines if current item is expanded (position == expandedPosition)
         Sets answer container visibility based on expansion state
         Rotates expand icon 90 degrees for expanded items
         Sets up click listener to toggle expansion
     */
    @Override
    public void onBindViewHolder(FAQViewHolder holder, int position) {
        FaqItem item = faqList.get(position);
        holder.bind(item, position, expandedPosition);
    }

    @Override
    public int getItemCount() {
        return faqList.size();
    }

    /*
         Replaces internal FAQ list with filtered results
         Resets expandedPosition to -1 (collapse all)
         Calls notifyDataSetChanged() to refresh UI
     */
    public void setFilteredList(List<FaqItem> filteredList) {
        this.faqList = filteredList;
        expandedPosition = -1;
        notifyDataSetChanged();
    }

    public class FAQViewHolder extends RecyclerView.ViewHolder {
        private TextView questionText;
        private TextView answerText;
        private ImageView expandIcon;
        private LinearLayout answerContainer;
        private LinearLayout faqItemContainer;

        public FAQViewHolder(View itemView) {
            super(itemView);
            questionText = itemView.findViewById(R.id.faqQuestion);
            answerText = itemView.findViewById(R.id.faqAnswer);
            expandIcon = itemView.findViewById(R.id.expandIcon);
            answerContainer = itemView.findViewById(R.id.answerContainer);
            faqItemContainer = itemView.findViewById(R.id.faqItemContainer);
        }

        /*
             Toggles expanded state when item clicked
             Notifies adapter of previous expanded item change (to collapse it)
             Notifies adapter of new expanded item change (to expand it)
             Only one item expanded at a time
         */
        public void bind(FaqItem item, int position, int expandedPosition) {
            questionText.setText(item.getQuestion());
            answerText.setText(item.getAnswer());

            boolean isExpanded = position == expandedPosition;
            answerContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            expandIcon.setRotation(isExpanded ? 90 : 0);

            faqItemContainer.setOnClickListener(view -> {
                int previousExpandedPosition = FaqAdapter.this.expandedPosition;
                FaqAdapter.this.expandedPosition = isExpanded ? -1 : position;

                if (previousExpandedPosition != -1) {
                    notifyItemChanged(previousExpandedPosition);
                }

                notifyItemChanged(position);
            });
        }
    }
}