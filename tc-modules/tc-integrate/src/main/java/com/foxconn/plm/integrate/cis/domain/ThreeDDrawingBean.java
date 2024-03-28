package com.foxconn.plm.integrate.cis.domain;

import lombok.Data;

import java.util.Date;

@Data
public class ThreeDDrawingBean {

    private String mfg;
    private String mfgPN;
    private String itemId;
    private String startTime;
    private String endTime;
    private String creator;
}
