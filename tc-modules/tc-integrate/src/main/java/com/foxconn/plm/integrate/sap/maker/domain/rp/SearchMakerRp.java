package com.foxconn.plm.integrate.sap.maker.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel
public class SearchMakerRp {

    @ApiModelProperty(value = "供应商编码")
    private String makerCode;


    @ApiModelProperty(value = "供应商名称")
    private String makerName;

    @ApiModelProperty(value = "供应商联系人")
    private String makerContact;
}
