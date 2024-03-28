package com.foxconn.plm.tcservice.tclicensereport.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class ReportVO {

    String BU;

    String dept;

    String func;

    @ApiModelProperty("license 总数量")
    int licenseTotal;

    @ApiModelProperty("未使用數量")
    int notUsedQty;

    @ApiModelProperty("使用率")
    float utilizationRate;

    @ApiModelProperty("累計使用時長")
    float accumulateUsageQty;

    @ApiModelProperty("中干License数量")
    int mainlandLicQty;

    @ApiModelProperty("中干工作天数")
    int mainlandWortDayQty;

    @ApiModelProperty("台干License数量")
    int taiwanLicQty;

    @ApiModelProperty("台干工作天数")
    int taiwanWortDayQty;

    @ApiModelProperty("稼動率")
    float cropRate;

    @ApiModelProperty("BU合并标记")
    String mergeFlag1;

    @ApiModelProperty("Dept合并标记")
    String mergeFlag2;

}
