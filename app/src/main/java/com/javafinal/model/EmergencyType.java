package com.javafinal.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Model ánh xạ bảng {@code emergency_types} trong CSDL (so_do_csdl.jpg)
 * và nhóm API Danh mục Loại Khẩn Cấp (#27 -> #31: {@code /api/emergency-types}).
 */
public class EmergencyType implements Serializable {

    public static final String CODE_NGAP_LUT = "NGAP_LUT";
    public static final String CODE_Y_TE = "Y_TE";
    public static final String CODE_MAC_KET = "MAC_KET";
    public static final String CODE_CHAY_NO = "CHAY_NO";
    public static final String CODE_NHU_YEU_PHAM = "NHU_YEU_PHAM";

    /** PK uuid */
    @SerializedName("id")
    private String id;

    /** UK varchar - Mã loại khẩn cấp chuẩn (VD: "NGAP_LUT", "Y_TE", "MAC_KET") */
    @SerializedName("name")
    private String name;

    /** text - Mô tả chi tiết tiếng Việt */
    @SerializedName("description")
    private String description;

    /** timestamptz - Thời điểm khởi tạo */
    @SerializedName("created_at")
    private String createdAt;

    public EmergencyType() {
    }

    public EmergencyType(String id, String name, String description, String createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getDisplayLabel() {
        return SosRequest.getEmergencyTypeLabel(name);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
