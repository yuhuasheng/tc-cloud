package com.foxconn.plm.spas.bean;

import lombok.Data;

@Data
public class SpasProductLinePhase {
    private Integer id;
    private Integer productLineId;
    private String phaseSn;
    private String name;
    private Integer isActive;
    private Integer businessStageId;
    private String updator;
    private String updateTime;
    private String creator;
    private String createTime;
}
