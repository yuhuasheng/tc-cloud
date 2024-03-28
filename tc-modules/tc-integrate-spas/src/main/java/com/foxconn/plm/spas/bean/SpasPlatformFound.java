package com.foxconn.plm.spas.bean;

import lombok.Data;

@Data
public class SpasPlatformFound {
    private Integer id;
    private Integer seriesId;
    private Integer productLineId;
    private String name;
    private Integer status;
    private Integer curPhaseId;
    private String closeCause;
    private Integer type;
    private Integer mainContact;
    private Integer isActive;
    private Integer owner;
    private String startTime;
    private String updator;
    private String updateTime;
    private String creator;
    private String createTime;
    private Integer process;
}
