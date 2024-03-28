package com.foxconn.plm.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class Entity {

    @ApiModelProperty(value = "创建人ID")
    String creatorId;

    @ApiModelProperty(value = "创建人名称")
    String creatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date created;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date lastUpdateDate;

    @ApiModelProperty(value = "修改人ID")
    String modifierId;

    @ApiModelProperty(value = "修改人名称")
    String modifierName;




}
