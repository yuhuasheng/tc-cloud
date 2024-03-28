package com.foxconn.plm.integrate.dgkpi.domain.resp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class PacDesignStandardResp {

    @ApiModelProperty(value = "专案id")
    private String spasProjId;

    @ApiModelProperty(value = "bom 名称")
    private String bomName;

    @ApiModelProperty(value = "特征")
    private String feature;

    @ApiModelProperty(value = "level")
    private String level;

    @ApiModelProperty(value = "HHPN")
    private String childId;

    @ApiModelProperty(value = "实际作者")
    private String actualUser;

}
