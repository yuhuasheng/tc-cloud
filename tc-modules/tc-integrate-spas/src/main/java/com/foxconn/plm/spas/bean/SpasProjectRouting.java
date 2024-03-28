package com.foxconn.plm.spas.bean;

import lombok.Data;


@Data
public class SpasProjectRouting {
    private Integer id;
    private String projectId;
    private Integer phaseId;
    private String phase;
    private Integer workItemStatus;
    private String workitem;
    private String startTime;
    private String endTime;
    private String createTime;
    private String updateTime;
    private Integer isActive;
}
