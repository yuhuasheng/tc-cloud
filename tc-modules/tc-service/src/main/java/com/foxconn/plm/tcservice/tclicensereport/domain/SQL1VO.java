package com.foxconn.plm.tcservice.tclicensereport.domain;

import lombok.Data;

@Data
public class SQL1VO {

    private String bu;
    private String dept;
    private String func;
    private String licenseTotal;
    private int mainlandWortDayQty;
    private int taiwanWortDayQty;
    private int cumulativeUsageDuration;

}
