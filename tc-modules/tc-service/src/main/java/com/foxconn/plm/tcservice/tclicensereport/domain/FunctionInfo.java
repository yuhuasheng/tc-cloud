package com.foxconn.plm.tcservice.tclicensereport.domain;

import lombok.Data;

import java.util.Date;

@Data
public class FunctionInfo {

    private String bu;
    private String department;
    private String function;
    private int usedHoursInDay;
    private float lurInDay;
    private Date recordDate;

}
