package com.foxconn.plm.integrate.dgkpi.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class RiskControlRp {


    @ApiModelProperty(value = "SPAS专案id")
    private String spasProjId;

    @ApiModelProperty(value = "文档类型")
    private String docType;
}
