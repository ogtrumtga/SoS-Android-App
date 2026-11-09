package com.javafinal.ui.rescuer;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.javafinal.R;
import com.javafinal.model.RescueTeam;
import com.javafinal.model.RescueTeamMember;
import com.javafinal.model.SosApiModels.RescueTeamsResponse;
import com.javafinal.model.TeamApiModels.MemberResponse;
import com.javafinal.model.User;
import com.javafinal.network.ApiError;
import com.javafinal.ui.ApiActivity;
import com.javafinal.util.NavigationHelper;
import com.javafinal.viewmodel.TeamViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Quyền quản lý lấy từ membership của từng đội và được backend kiểm tra lại khi ghi. */
public class RescueTeamActivity extends ApiActivity {
    private static final String[] ROLES = {"MEMBER", "LEADER", "DRIVER", "MEDIC"};
    private TeamViewModel teams;
    private TeamMemberAdapter adapter;
    private Spinner teamSpinner;
    private MaterialToolbar toolbar;
    private List<RescueTeam> myTeams = Collections.emptyList();
    private String selectedTeamId;
    private boolean loading, writing, permissionsReady, reloadPending;
    private BottomSheetDialog editor;
    private View editorView;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setupScreen(R.layout.activity_rescue_team);
        if (!requireSession()) return;
        User user = factory.getSession().getUser();
        if (user == null) { goToLogin(); return; }
        if (!user.isRescuer()) { goToRoleHome(user); return; }
        if (state != null) selectedTeamId = state.getString("team_id");
        teams = vm(TeamViewModel.class);
        toolbar = findViewById(R.id.teamToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.getMenu().add(Menu.NONE, 1, Menu.NONE, "Tải lại").setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM);
        toolbar.setOnMenuItemClickListener(item -> { if (!loading && !writing) loadTeams(); return true; });
        teamSpinner = findViewById(R.id.spinnerTeams);
        adapter = new TeamMemberAdapter(user.getId(), new TeamMemberAdapter.Actions() {
            @Override public void edit(RescueTeamMember member) { showEditor(member); }
            @Override public void delete(RescueTeamMember member) { confirmDelete(member); }
        });
        RecyclerView list = findViewById(R.id.rvTeamMembers);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        teamSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < myTeams.size()) {
                    selectedTeamId = myTeams.get(position).getTeamId();
                    render();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        findViewById(R.id.btnAddTeamMember).setOnClickListener(v -> showEditor(null));

        bind("teams", (RescueTeamsResponse response) -> {
            myTeams = response.getData() == null ? Collections.emptyList() : response.getData();
            permissionsReady = true;
            if (selectedTeam() == null) selectedTeamId = myTeams.isEmpty() ? null : myTeams.get(0).getTeamId();
            List<String> names = new ArrayList<>();
            int selected = 0;
            for (int i = 0; i < myTeams.size(); i++) {
                RescueTeam team = myTeams.get(i);
                names.add(team.getName());
                if (team.getTeamId().equals(selectedTeamId)) selected = i;
            }
            ArrayAdapter<String> choices = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            choices.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            teamSpinner.setAdapter(choices);
            teamSpinner.setSelection(selected);
            clearStatus();
            render();
            if (reloadPending) { reloadPending = false; loadTeams(); }
        }, error -> {
            reloadPending = false;
            permissionsReady = false;
            if (error.getStatusCode() == 403) {
                myTeams = Collections.emptyList();
                selectedTeamId = null;
            }
            render();
            showApiError(error, this::loadTeams);
        }, busy -> {
            loading = busy;
            if (busy) { permissionsReady = false; showLoading("Đang tải đội viên…"); }
            render();
        });
        bind("member_write", (MemberResponse response) -> {
            dismissEditor();
            NavigationHelper.showSnack(this, response.getMessage() == null ? "Đã cập nhật đội viên." : response.getMessage());
            loadTeams();
        }, this::writeError, busy -> {
            writing = busy;
            render();
            updateEditor();
            if (busy) showLoading("Đang lưu thay đổi…");
        });
        loadTeams();
    }

    private void loadTeams() {
        if (loading) { reloadPending = true; return; }
        execute("teams", teams::loadMyTeams);
    }

    private RescueTeam selectedTeam() {
        for (RescueTeam team : myTeams) if (team.getTeamId().equals(selectedTeamId)) return team;
        return null;
    }

    private boolean canManage() {
        RescueTeam team = selectedTeam();
        return permissionsReady && team != null && team.canManage();
    }

    private void render() {
        if (adapter == null) return;
        RescueTeam team = selectedTeam();
        boolean busy = loading || writing;
        teamSpinner.setVisibility(myTeams.size() > 1 ? View.VISIBLE : View.GONE);
        teamSpinner.setEnabled(!busy);
        toolbar.getMenu().findItem(1).setEnabled(!busy);
        ((TextView) findViewById(R.id.tvRescueTeamName)).setText(team == null ? "Đội cứu hộ" : team.getName());
        TextView description = findViewById(R.id.tvRescueTeamDescription);
        description.setText(team == null ? "" : team.getDescription());
        description.setVisibility(team == null || team.getDescription() == null || team.getDescription().isEmpty() ? View.GONE : View.VISIBLE);
        TextView permission = findViewById(R.id.tvTeamPermission);
        permission.setText(team == null ? "" : "Vai trò của bạn: " + RescueTeamMember.roleLabel(team.getCurrentUserRole())
                + (canManage() ? "\nBạn có thể thêm, đổi vai trò và xóa đội viên." : ""));
        View add = findViewById(R.id.btnAddTeamMember);
        add.setVisibility(canManage() ? View.VISIBLE : View.GONE);
        add.setEnabled(!busy);
        List<RescueTeamMember> members = team == null || team.getMembers() == null ? Collections.emptyList() : team.getMembers();
        ((TextView) findViewById(R.id.tvTeamMemberCount)).setText("DANH SÁCH ĐỘI VIÊN · " + members.size());
        findViewById(R.id.tvTeamEmpty).setVisibility(!loading && team == null ? View.VISIBLE : View.GONE);
        adapter.submit(members, canManage(), busy);
    }

    private void showEditor(RescueTeamMember member) {
        if (!canManage() || loading || writing) return;
        final String teamId = selectedTeamId;
        editor = new BottomSheetDialog(this);
        editorView = getLayoutInflater().inflate(R.layout.sheet_team_member, null);
        ((TextView) editorView.findViewById(R.id.tvMemberEditorTitle)).setText(member == null ? "Thêm đội viên" : "Sửa vai trò");
        ((TextView) editorView.findViewById(R.id.tvMemberEditorHint)).setText(member == null
                ? "Nhập số điện thoại tài khoản đã đăng ký để thêm vào đội."
                : text(member.getName()) + " · " + text(member.getPhone()));
        editorView.findViewById(R.id.inputMemberPhoneLayout).setVisibility(member == null ? View.VISIBLE : View.GONE);
        Spinner role = editorView.findViewById(R.id.spinnerMemberRole);
        List<String> roleNames = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < ROLES.length; i++) {
            roleNames.add(RescueTeamMember.roleLabel(ROLES[i]));
            if (member != null && ROLES[i].equals(member.getRole())) selected = i;
        }
        ArrayAdapter<String> roles = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roleNames);
        roles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        role.setAdapter(roles);
        role.setSelection(selected);
        editorView.findViewById(R.id.btnCancelTeamMember).setOnClickListener(v -> dismissEditor());
        editorView.findViewById(R.id.btnSaveTeamMember).setOnClickListener(v -> {
            if (writing || !canManage()) return;
            editorView.findViewById(R.id.tvMemberEditorError).setVisibility(View.GONE);
            String chosenRole = ROLES[role.getSelectedItemPosition()];
            TextInputEditText input = editorView.findViewById(R.id.inputMemberPhone);
            String phone = input.getText() == null ? "" : input.getText().toString().trim();
            execute("member_write", () -> member == null ? teams.addMember(teamId, phone, chosenRole)
                    : teams.updateMember(teamId, member.getMemberId(), chosenRole));
        });
        editor.setContentView(editorView);
        editor.setOnDismissListener(dialog -> { editor = null; editorView = null; });
        editor.show();
    }

    private void confirmDelete(RescueTeamMember member) {
        if (!canManage() || loading || writing) return;
        final String teamId = selectedTeamId;
        NavigationHelper.showConfirmSheet(this, null, "Xóa đội viên?",
                text(member.getName()) + " (" + text(member.getPhone()) + ") sẽ bị xóa khỏi đội. Tài khoản của người này vẫn được giữ lại.",
                "Xóa khỏi đội", true, () -> {
                    if (canManage() && !writing) execute("member_write", () -> teams.deleteMember(teamId, member.getMemberId()));
                }, "Hủy", null);
    }

    private void writeError(ApiError error) {
        clearStatus();
        if (error.isUnauthorized() || error.getStatusCode() == 403) {
            dismissEditor();
            showApiError(error, null);
            if (!error.isUnauthorized()) { NavigationHelper.showSnack(this, error.getMessage()); loadTeams(); }
        } else if (editorView != null) {
            TextView errorText = editorView.findViewById(R.id.tvMemberEditorError);
            errorText.setText(error.getMessage());
            errorText.setVisibility(View.VISIBLE);
        } else showApiError(error, null);
    }

    private void updateEditor() {
        if (editorView == null) return;
        editorView.findViewById(R.id.btnSaveTeamMember).setEnabled(!writing);
        editorView.findViewById(R.id.btnCancelTeamMember).setEnabled(!writing);
        editorView.findViewById(R.id.inputMemberPhone).setEnabled(!writing);
        editorView.findViewById(R.id.spinnerMemberRole).setEnabled(!writing);
        editor.setCancelable(!writing);
        editor.setCanceledOnTouchOutside(!writing);
    }

    private void dismissEditor() { if (editor != null) editor.dismiss(); }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("team_id", selectedTeamId);
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() { dismissEditor(); super.onDestroy(); }
}
