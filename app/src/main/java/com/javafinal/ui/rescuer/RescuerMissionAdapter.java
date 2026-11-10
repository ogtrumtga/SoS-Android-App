package com.javafinal.ui.rescuer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.javafinal.R;
import com.javafinal.model.SosRequest;
import com.javafinal.util.ApiDisplay;

import java.util.ArrayList;
import java.util.List;

public class RescuerMissionAdapter extends RecyclerView.Adapter<RescuerMissionAdapter.MissionViewHolder> {

    public interface OnMissionClickListener {
        void onMissionClick(SosRequest mission, boolean isCurrent);
    }

    private final List<SosRequest> items = new ArrayList<>();
    private final OnMissionClickListener listener;
    private String activeMissionId;

    public RescuerMissionAdapter(OnMissionClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<SosRequest> newItems, String currentActiveId) {
        this.activeMissionId = currentActiveId;
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public boolean isCurrentMission(SosRequest item) {
        if (item == null) return false;
        if (activeMissionId != null && item.getId() != null && activeMissionId.equals(item.getId())) {
            return true;
        }
        String status = item.getStatus();
        return SosRequest.STATUS_ACCEPTED.equals(status)
                || SosRequest.STATUS_RESCUING.equals(status)
                || SosRequest.STATUS_ARRIVED.equals(status);
    }

    @NonNull
    @Override
    public MissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rescuer_mission, parent, false);
        return new MissionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MissionViewHolder holder, int position) {
        SosRequest req = items.get(position);
        Context context = holder.itemView.getContext();
        boolean isCurrent = isCurrentMission(req);
        String status = req.getStatus();

        if (isCurrent) {
            holder.cardMission.setCardBackgroundColor(ContextCompat.getColor(context, R.color.red_soft));
            holder.cardMission.setStrokeColor(ContextCompat.getColor(context, R.color.red));
            holder.cardMission.setStrokeWidth(dp(context, 1.5f));

            holder.tvStatusBadge.setText("● Ca hiện tại · " + req.getStatusLabel());
            holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.red)));
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.imgChevron.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.red)));
        } else {
            holder.cardMission.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
            holder.cardMission.setStrokeColor(ContextCompat.getColor(context, R.color.line));
            holder.cardMission.setStrokeWidth(dp(context, 1f));

            if (SosRequest.STATUS_COMPLETED.equals(status)) {
                holder.tvStatusBadge.setText("Hoàn thành");
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.green_soft)));
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.green));
            } else if ("RELEASED".equals(status)) {
                holder.tvStatusBadge.setText("Đã nhả ca");
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.amber_soft)));
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.amber));
            } else {
                holder.tvStatusBadge.setText(req.getStatusLabel());
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.fill)));
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.ink_secondary));
            }
            holder.imgChevron.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.ink_tertiary)));
        }

        holder.tvMissionTitle.setText(req.getEmergencyTypeLabel() + " · " + req.getShortId());
        holder.tvMissionMeta.setText(safe(req.getRequesterName()) + " • " + safe(req.getRequesterPhone()));

        String timeSource = !isCurrent && req.getUpdatedAt() != null
                ? req.getUpdatedAt() : req.getCreatedAt();
        holder.tvMissionTime.setText(ApiDisplay.shortTime(timeSource));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMissionClick(req, isCurrent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    static class MissionViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardMission;
        final TextView tvStatusBadge;
        final TextView tvMissionTime;
        final TextView tvMissionTitle;
        final TextView tvMissionMeta;
        final ImageView imgChevron;

        MissionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMission = itemView.findViewById(R.id.cardMission);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvMissionTime = itemView.findViewById(R.id.tvMissionTime);
            tvMissionTitle = itemView.findViewById(R.id.tvMissionTitle);
            tvMissionMeta = itemView.findViewById(R.id.tvMissionMeta);
            imgChevron = itemView.findViewById(R.id.imgChevron);
        }
    }
}
