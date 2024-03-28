package com.foxconn.plm.tcservice.tcfr;

import lombok.Data;

@Data
public class TCFRReportDetailBean {

    private String bu;
    private String customer;
    private String productLine;
    private String series;
    private String projectName;
    private String currentPhase;
    private String phase;
    private String overallNotHeldRate;
    private String designReviewMeeting;
    private String startDate;
    private String endDate;
    private boolean isReduplicate;
    private int numOfNotHeld;
    private int numOfShouldHeld;
    private String numOfNotHeldRate;
    private String spm;
    private String pid;
    private String needNotifyWeek;
    private String year;

}
