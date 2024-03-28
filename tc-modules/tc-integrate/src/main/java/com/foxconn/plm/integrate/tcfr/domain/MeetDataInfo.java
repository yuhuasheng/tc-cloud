package com.foxconn.plm.integrate.tcfr.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author HuashengYu
 * @Date 2023/3/7 17:27
 * @Version 1.0
 */
@Data
@ApiModel
public class MeetDataInfo implements Comparable<MeetDataInfo> {

    @ApiModelProperty(value = "任务名称")
    private String actionItem;

    @ApiModelProperty(value = "任务Id")
    private String actionItemId;

    @ApiModelProperty(value = "计划开始时间")
    private String planStartDate;

    @ApiModelProperty(value = "计划结束时间")
    private String planEndDate;

    @ApiModelProperty(value = "所有者")
    private String owners;


    @Override
    public int compareTo(MeetDataInfo o) {
//        int i = o.getPlanStartDate().compareTo(this.planStartDate);
//        if (i == 0) {
//            return o.getPlanEndDate().compareTo(this.planEndDate);
//        }
//        return i;
        return 0;
    }
}
