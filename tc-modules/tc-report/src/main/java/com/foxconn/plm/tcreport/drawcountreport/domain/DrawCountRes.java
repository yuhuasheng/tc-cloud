package com.foxconn.plm.tcreport.drawcountreport.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @ClassName: DrawCountRes
 * @Description:
 * @Author DY
 * @Create 2023/1/7
 */
@Data
@EqualsAndHashCode
public class DrawCountRes implements Serializable {
    @ApiModelProperty("BU")
    private String bu;
    @ApiModelProperty("Customer")
    private String customer;
    @ApiModelProperty("ProductLine")
    private String productLine;
    @ApiModelProperty("ProjectSeries")
    private String projectSeries;
    @ApiModelProperty("ProjectName")
    private String projectName;
    @ApiModelProperty("协同结构树类别")
    private String designTreeType;
    @ApiModelProperty("协同结构树名称")
    private String designTreeName;
    @ApiModelProperty("所有者")
    private String owner;
    @ApiModelProperty("所在组")
    private String ownerGroup;
    @ApiModelProperty("实际工作者")
    private String actualUser;
    @ApiModelProperty("零件编码")
    private String itemCode;
    @ApiModelProperty("零件名称")
    private String itemName;
    @ApiModelProperty("零件类别")
    private String itemType;
    @ApiModelProperty("应上传数量")
    private Integer uploadNum;
    @ApiModelProperty("已发布数量")
    private Integer releaseNum;
    @ApiModelProperty("发布进度")
    private String releaseProgress;
    @ApiModelProperty("发布3D模型数量")
    private Integer releaseModelNum;
    @ApiModelProperty("3D零件完整度")
    private String itemCompleteness;
    @ApiModelProperty("3D图档完整度")
    private String drawCompleteness;
    @ApiModelProperty("專案id")
    private String projectId;
    private String phase;
    private String chassis;

    public DrawCountRes() {
    }

    public DrawCountRes(DrawCountBean bean) {
        this.bu = bean.getBu();
        this.customer = bean.getCustomer();
        this.productLine = bean.getProductLine();
        this.projectSeries = bean.getProjectSeries();
        this.projectName = bean.getProjectName();
        this.designTreeType = bean.getDesignTreeType();
        this.designTreeName = bean.getDesignTreeName();
        this.owner = bean.getOwner();
        this.ownerGroup = bean.getOwnerGroup();
        this.actualUser = bean.getPractitioner();
        this.itemCode = bean.getItemCode();
        this.itemName = bean.getItemName();
        this.itemType = bean.getItemType();
        this.projectId = bean.getProjectId();
        this.phase = bean.getPhase();
        this.chassis = bean.getChassis();
        this.uploadNum = 0;
        this.releaseNum = 0;
        this.releaseModelNum = 0;
    }
}
