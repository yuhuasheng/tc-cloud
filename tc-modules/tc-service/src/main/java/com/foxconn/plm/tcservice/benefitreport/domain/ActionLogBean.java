package com.foxconn.plm.tcservice.benefitreport.domain;

import lombok.Data;

import java.util.Date;

@Data
public class ActionLogBean {
    private String functionName;
    private String creator;
    private String creatorName;
    private String project;
    private String phase;
    private String itemId;
    private String itemRevUid;
    private String startTime;
    private String endTime;
    private String itemRev;
    private String bu;
    private String dept;
    private String projLevel;
    private String handleResult;
    private String exceptionMsg;
    private String custom;
    private String phaseEndDate;
}
