package com.foxconn.plm.tcservice.tclicensereport.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class QueryRp {

    @ApiModelProperty
    String bu;

    @ApiModelProperty
    String dept;

    @ApiModelProperty
    String func;

    @ApiModelProperty
    String startDate;

    @ApiModelProperty
    String endDate;

    @ApiModelProperty
    String historyMouth;

}
