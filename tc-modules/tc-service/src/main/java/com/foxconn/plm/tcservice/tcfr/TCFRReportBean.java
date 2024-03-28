package com.foxconn.plm.tcservice.tcfr;

import lombok.Data;

@Data
public class TCFRReportBean {

    private String bu;
    private String customer;
    private String productLine;
    private int numOfProject;
    private int W0;
    private int W1;
    private int W2;
    private int W3;
    private int numOfAccumulatedHeld;
    private int numOfAccumulatedNotHeld;
    private int numOfShouldHeld;
    private String rateOfHeld;

}
