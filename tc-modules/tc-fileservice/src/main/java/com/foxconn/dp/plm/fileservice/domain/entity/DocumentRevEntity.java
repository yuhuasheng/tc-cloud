package com.foxconn.dp.plm.fileservice.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel
public class DocumentRevEntity {

    @ApiModelProperty(value = "文档版本id")
    private Long docRevId;

    @ApiModelProperty(value = "文檔id")
    private Long docId;

    @ApiModelProperty(value = "文件夹id")
    private Long folderId;


    @ApiModelProperty(value = "文檔名稱")
    private String docRevName;


    @ApiModelProperty(value = "文檔版本号")
    private String docRevNum;

    @ApiModelProperty(value = "引用类型 0：文件系統；1：TC")
    private Integer refType;

    @ApiModelProperty(value = "引用id")
    private String refId;

    @ApiModelProperty(value = "生命週期階段 0：已發行；1：未發行；2：廢棄 4：审核中")
    private Integer lifecyclePhase;

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
