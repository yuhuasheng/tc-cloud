package com.foxconn.plm.tcservice.materialInfo.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel
public class MaterialGroupAndBaseUnitRp {

    @ApiModelProperty(value = "料号")
    private String materialNum;


}
