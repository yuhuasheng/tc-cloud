package com.foxconn.plm.integrate.dgkpi.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel
public class UrlRp {


    @ApiModelProperty(value = "SPAS专案id")
    private String spasProjId;

    @ApiModelProperty(value = "文档类型")
    private String docType;

    @ApiModelProperty(value = "對象ID")
    private String itemId;

    @ApiModelProperty(value = "對象版本規則")
    private String revsionRule;


    @ApiModelProperty(value = "對象查詢條件")
    private String objectCondition;

}
