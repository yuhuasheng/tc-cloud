package com.foxconn.plm.tcservice.tclicensereport.domain;

import lombok.Data;

import java.util.Date;

@Data
public class UserInfo {

    private String bu;
    private String department;
    private String function;
    private String userId;
    private String userName;
    private Date lastLoginDate;
    private Integer usedHoursInMonth;
    private Integer usedHoursInDay;
    private Date recordDate;

}
