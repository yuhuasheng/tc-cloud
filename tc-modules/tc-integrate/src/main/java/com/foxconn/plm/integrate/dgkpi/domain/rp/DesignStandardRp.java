package com.foxconn.plm.integrate.dgkpi.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class DesignStandardRp {

    @ApiModelProperty(value = "图号")
    private String itemId;

    @ApiModelProperty(value = "专案id")
    private String spasProjId;

    @ApiModelProperty(value = "部门")
    private String function;
}
