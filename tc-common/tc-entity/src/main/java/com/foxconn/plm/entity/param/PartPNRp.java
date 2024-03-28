package com.foxconn.plm.entity.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel
public class PartPNRp {
    @ApiModelProperty(value = "itemNumber")
    private String itemNumber;

    @ApiModelProperty(value = "plant")
    private String plant;

}
