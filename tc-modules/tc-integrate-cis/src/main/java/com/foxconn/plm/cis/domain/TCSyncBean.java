package com.foxconn.plm.cis.domain;

import lombok.Data;

import java.util.Date;

/**
 * @Author HuashengYu
 * @Date 2022/9/3 10:51
 * @Version 1.0
 */
@Data
public class TCSyncBean {
    private String mfg;
    private String mfgPN;
    private String HHPN;
    private String schematicPart;
    private String pcbFootprint;
    private Date startTime;
    private Date endTime;
    private String creator;
}
