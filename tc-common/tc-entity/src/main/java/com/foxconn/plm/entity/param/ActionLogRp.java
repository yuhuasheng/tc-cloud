package com.foxconn.plm.entity.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class ActionLogRp {

    @ApiModelProperty(value = "id", hidden = true)
    private Long id;

    @ApiModelProperty(value = "BU")
    private String bu;

    @ApiModelProperty(value = "功能名稱")
    private String functionName;

    @ApiModelProperty(value = "創建人")
    private String creator;

    @ApiModelProperty(value = "創建人姓名")
    private String creatorName;

    @ApiModelProperty(value = "專案")
    private String project;

    @ApiModelProperty(value = "階段")
    private String phase;

    @ApiModelProperty(value = "料號")
    private String itemId;

    @ApiModelProperty(value = "版本")
    private String rev;

    @ApiModelProperty(value = "版本uid")
    private String revUid;

    @ApiModelProperty(value = "開始時間 yyyy-MM-dd hh:mm:ss:SSS")
    private String startTime;

    @ApiModelProperty(value = "結束時間 yyyy-MM-dd hh:mm:ss:SSS")
    private String endTime;

    private String userName;
}
