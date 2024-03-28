package com.foxconn.plm.tcservice.tclicensereport.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class UserInfoVO {
    String BU;

    String dept;

    String func;

    @ApiModelProperty("工号")
    String workNum;

    @ApiModelProperty("姓名")
    String name;

    @ApiModelProperty("最近登陆时间")
    String loginDate;

    @ApiModelProperty("当日累计使用小时数")
    float accumulateDayUsageQty;

}
