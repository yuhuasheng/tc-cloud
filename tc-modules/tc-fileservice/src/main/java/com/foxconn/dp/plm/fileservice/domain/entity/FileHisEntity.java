package com.foxconn.dp.plm.fileservice.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 文件操作历史
 */
@Data
@ApiModel
public class FileHisEntity {


    private Long fileHisId;

    @ApiModelProperty(value = "文件版本Id")
    private Long fileVersionId;//

    @ApiModelProperty(value = "0：新增；1：刪除")
    private Integer hisAction;

    @ApiModelProperty(value = "操作描述")
    private String hisDescription;//

    @ApiModelProperty(value = "修改人")
    private String modified;

    @ApiModelProperty(value = "創建日期")
    private Date created;

}
