package com.javafinal.model;

import com.google.gson.annotations.SerializedName;

public final class TeamApiModels {
    private TeamApiModels() { }

    public static final class AddMemberRequest {
        @SerializedName("phone") private final String phone;
        @SerializedName("role") private final String role;
        public AddMemberRequest(String phone, String role) { this.phone = phone; this.role = role; }
    }

    public static final class UpdateMemberRequest {
        @SerializedName("role") private final String role;
        public UpdateMemberRequest(String role) { this.role = role; }
    }

    public static final class MemberResponse {
        @SerializedName("message") private String message;
        @SerializedName("data") private RescueTeamMember data;
        public String getMessage() { return message; }
        public RescueTeamMember getData() { return data; }
    }
}
