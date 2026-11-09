package com.javafinal.viewmodel;

import androidx.lifecycle.LiveData;
import com.javafinal.model.RescueTeamMember;
import com.javafinal.model.SosApiModels.RescueTeamsResponse;
import com.javafinal.model.TeamApiModels.*;
import com.javafinal.network.ApiClient;

public final class TeamViewModel extends BaseApiViewModel {
    public TeamViewModel(ApiClient api) { super(api); }

    public LiveData<ApiState<RescueTeamsResponse>> loadMyTeams() {
        return request("GET", "api/rescuer/teams/mine", null, true, RescueTeamsResponse.class, null, null);
    }

    public LiveData<ApiState<MemberResponse>> addMember(String teamId, String phone, String role) {
        return request("POST", () -> membersPath(teamId),
                new AddMemberRequest(phone == null ? null : phone.trim(), role), true, MemberResponse.class,
                () -> { InputValidator.phone(phone); validateRole(role); }, null);
    }

    public LiveData<ApiState<MemberResponse>> updateMember(String teamId, String memberId, String role) {
        return request("PUT", () -> membersPath(teamId) + "/" + ApiClient.segment(memberId),
                new UpdateMemberRequest(role), true, MemberResponse.class, () -> validateRole(role), null);
    }

    public LiveData<ApiState<MemberResponse>> deleteMember(String teamId, String memberId) {
        return request("DELETE", () -> membersPath(teamId) + "/" + ApiClient.segment(memberId),
                null, true, MemberResponse.class, null, null);
    }

    private String membersPath(String teamId) { return "api/rescuer/teams/" + ApiClient.segment(teamId) + "/members"; }
    private void validateRole(String role) {
        InputValidator.oneOf(role, "Vai trò trong đội", RescueTeamMember.ROLE_MEMBER,
                RescueTeamMember.ROLE_LEADER, RescueTeamMember.ROLE_DRIVER, RescueTeamMember.ROLE_MEDIC);
    }
}
