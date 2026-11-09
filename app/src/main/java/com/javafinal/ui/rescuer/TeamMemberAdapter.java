package com.javafinal.ui.rescuer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.javafinal.R;
import com.javafinal.model.RescueTeamMember;
import java.util.Collections;
import java.util.List;

final class TeamMemberAdapter extends RecyclerView.Adapter<TeamMemberAdapter.Holder> {
    interface Actions {
        void edit(RescueTeamMember member);
        void delete(RescueTeamMember member);
    }
    private final Actions actions;
    private final String currentUserId;
    private List<RescueTeamMember> members = Collections.emptyList();
    private boolean canManage, busy;

    TeamMemberAdapter(String currentUserId, Actions actions) {
        this.currentUserId = currentUserId;
        this.actions = actions;
    }

    void submit(List<RescueTeamMember> members, boolean canManage, boolean busy) {
        this.members = members == null ? Collections.emptyList() : members;
        this.canManage = canManage;
        this.busy = busy;
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_team_member, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        RescueTeamMember member = members.get(position);
        String name = member.getName() == null ? "Đội viên" : member.getName();
        holder.name.setText(name + (currentUserId != null && currentUserId.equals(member.getUserId()) ? " (Bạn)" : ""));
        holder.phone.setText(member.getPhone());
        holder.role.setText(RescueTeamMember.roleLabel(member.getRole()));
        holder.actions.setVisibility(canManage ? View.VISIBLE : View.GONE);
        holder.edit.setEnabled(!busy);
        holder.delete.setEnabled(!busy);
        holder.edit.setOnClickListener(v -> actions.edit(member));
        holder.delete.setOnClickListener(v -> actions.delete(member));
    }

    @Override public int getItemCount() { return members.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name, phone, role;
        final View actions, edit, delete;
        Holder(View view) {
            super(view);
            name = view.findViewById(R.id.tvMemberName);
            phone = view.findViewById(R.id.tvMemberPhone);
            role = view.findViewById(R.id.tvMemberRole);
            actions = view.findViewById(R.id.memberActions);
            edit = view.findViewById(R.id.btnEditMember);
            delete = view.findViewById(R.id.btnDeleteMember);
        }
    }
}
