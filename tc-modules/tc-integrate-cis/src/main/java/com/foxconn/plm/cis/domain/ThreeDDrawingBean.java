package com.foxconn.plm.cis.domain;

import lombok.Data;

@Data
public class ThreeDDrawingBean {

    private String mfg;
    private String mfgPN;
    private String itemId;
    private String startTime;
    private String endTime;
    private String creator;
}
