package com.foxconn.plm.integrate.dgkpi.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class PacDesignStandardRp {

    @ApiModelProperty(value = "专案id")
    private String spasProjId;

    @ApiModelProperty(value = "部门")
    private String function;

    @ApiModelProperty(value = "bom type")
    private String bomType;


    @ApiModelProperty(value = "阶段")
    private String phase;


}
