package com.foxconn.dp.plm.fileservice.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class ServerListEntity {

    @ApiModelProperty(value = "服务id")
    private Integer id;

    @ApiModelProperty(value = "ip地址")
    private String value;

    @ApiModelProperty(value = "服务编码")
    private String code;


}
