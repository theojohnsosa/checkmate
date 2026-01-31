package com.example.authtest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

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

    @Override
    public void onBindViewHolder(FAQViewHolder holder, int position) {
        FaqItem item = faqList.get(position);
        holder.bind(item, position, expandedPosition);
    }

    @Override
    public int getItemCount() {
        return faqList.size();
    }

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

        public void bind(FaqItem item, int position, int expandedPosition) {
            questionText.setText(item.getQuestion());
            answerText.setText(item.getAnswer());

            boolean isExpanded = position == expandedPosition;
            answerContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            expandIcon.setRotation(isExpanded ? 90 : 0);

            faqItemContainer.setOnClickListener(v -> {
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