package com.cyberaware.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<Report> reportList;
    private Context context;

    public HistoryAdapter(Context context, List<Report> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = reportList.get(position);

        // Set basic data
        holder.tvMessage.setText(report.getMessage());
        holder.tvStatus.setText(report.getAiResult());
        holder.tvSender.setText(report.getSender()); // Sender নাম দেখানো

        // Color Logic
        if ("SCAM".equals(report.getAiResult()) || "SUSPICIOUS".equals(report.getAiResult())) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.alert_crimson));
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.alert_crimson));
        } else {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.hacker_green));
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.hacker_green));
        }

        // 🔥 Click Listener: Open Detail Page
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ScanDetailActivity.class);
            intent.putExtra("sender", report.getSender());
            intent.putExtra("message", report.getMessage());
            intent.putExtra("status", report.getAiResult());
            intent.putExtra("time", report.getTimestamp());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus, tvMessage, tvSender;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvMessage = itemView.findViewById(R.id.tvMessagePreview);
            ivIcon = itemView.findViewById(R.id.ivStatusIcon);
            // item_history.xml এ Sender এর জন্য TextView না থাকলে নিচের লাইনটি কমেন্ট করে রাখুন
            tvSender = itemView.findViewById(R.id.tvSender);
        }
    }
}