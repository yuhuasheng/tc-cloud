package com.foxconn.plm.tcservice.materialInfo.domain.rp;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class MaterialInfoPojo {
    @ApiModelProperty(value = "料号")
    private String materialNum;
    @ApiModelProperty(value = "物料群组")
    private String materialGroup;
    @ApiModelProperty(value = "基本单位")
    private String baseUnit;


}
