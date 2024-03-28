package com.foxconn.dp.plm.fileservice.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel
public class DocumentEntity {

    @ApiModelProperty(value = "文档id")
    private Long docId;

    @ApiModelProperty(value = "文檔編號")
    private String docNum;

    @ApiModelProperty(value = "文檔名稱")
    private String docName;

    @ApiModelProperty(value = "文档来源 0：文件系統；1：TC")
    private Integer docOrigin;

    @ApiModelProperty(value = "文档描述")
    private String docDescription;

    @ApiModelProperty(value = "文档类型")
    private String docCategory;

    @ApiModelProperty(value = "產品代碼")
    private String productCode;

    @ApiModelProperty(value = "產品線")
    private String productLine;

    @ApiModelProperty(value = "客戶")
    private String customer;

    @ApiModelProperty(value = "0：未删除；1：已删除")
    private Integer delFlag;

    @ApiModelProperty(value = "创建日期")
    private Date createDate;

    @ApiModelProperty(value = "更新日期")
    private Date lastUpdateDate;

    @ApiModelProperty(value = "创建人工号")
    private String creator;

    @ApiModelProperty(value = "创建人姓名")
    private String creatorName;

}
