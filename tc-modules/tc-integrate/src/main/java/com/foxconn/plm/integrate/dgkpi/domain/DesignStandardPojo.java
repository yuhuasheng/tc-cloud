package com.foxconn.plm.integrate.dgkpi.domain;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
public class DesignStandardPojo {


    @ApiModelProperty(value = "Parent ID")
    private String parentId;

    @ApiModelProperty(value = "Child ID")
    private String childId;

    @ApiModelProperty(value = "特徵")
    private String feature = "";

    @ApiModelProperty(value = "狀態")
    private String status = "";

    @ApiModelProperty(value = "首用專案ID")
    private String initialProjId = "";

    @ApiModelProperty(value = "所屬專案")
    private String spasProjIds = "";

    @ApiModelProperty(value = "實際作者")
    private String actualUser = "";

    @ApiModelProperty(value = "角色")
    private String userRole = "";

    @ApiModelProperty(value = "ME類型")
    private String meType = "";

    @ApiModelProperty(value = "EMC類型")
    private String emcType = "";

    @ApiModelProperty(value = "用量")
    private String qty;

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getEmcType() {
        return emcType;
    }

    public void setEmcType(String emcType) {
        this.emcType = emcType;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public String getFeature() {
        if (feature == null) {
            return "";
        }
        if (feature.endsWith(",")) {
            return feature.substring(0, feature.length() - 1);
        }
        return feature.trim();
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInitialProjId() {
        return initialProjId;
    }

    public void setInitialProjId(String initialProjId) {
        this.initialProjId = initialProjId;
    }

    public String getSpasProjIds() {
        return spasProjIds;
    }

    public void setSpasProjIds(String spasProjIds) {
        this.spasProjIds = spasProjIds;
    }

    public String getActualUser() {
        return actualUser;
    }

    public void setActualUser(String actualUser) {
        this.actualUser = actualUser;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getMeType() {
        if (meType == null) {
            return "";
        }
        if (meType.endsWith(",")) {
            return meType.substring(0, meType.length() - 1);
        }

        return meType.trim();
    }


    public void addMeType(String kType) {
        if (kType == null || "".equalsIgnoreCase(kType.trim())) {
            meType = "";
            return;
        }

        String[] lps = meType.split(",");
        int f = 0;
        for (String s : lps) {
            if (s.equalsIgnoreCase(kType)) {
                f = 1;
                break;
            }
        }

        if (f == 0) {
            if (meType == null) {
                meType = "";
            }
            meType += kType + ",";
        }

    }
}
