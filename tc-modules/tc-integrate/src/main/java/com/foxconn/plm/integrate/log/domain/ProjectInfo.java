package com.foxconn.plm.integrate.log.domain;

import lombok.Data;

@Data
public class ProjectInfo {
    private String projId;
    private String bu;
    private String customer;
    private String productLine;
    private String series;
    private String projName;
    private String phase;
    private String curPhase;
    private String startTime;
    private String endTime;
    private String curStartTime;
    private String curEndTime;
    private String ownerName;

}
