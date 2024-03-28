package com.foxconn.plm.tcservice.tclicensereport.domain;

import lombok.Data;

@Data
public class TCLicenseByBean {

    private String bu;
    private String department;
    private String function;
    private String level;
    private String customer;
    private String phase;
    private String avgUsed;
    private String avgRate;


}
