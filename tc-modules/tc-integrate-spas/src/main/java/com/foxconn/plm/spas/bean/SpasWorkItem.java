package com.foxconn.plm.spas.bean;

import lombok.Data;

import java.util.Date;

@Data
public class SpasWorkItem {

    private Integer id;
    private String userId;
    private String group;
    private String function;
    private String customer;
    private String productLine;
    private String projectId;
    private String curPhase;
    private Integer workItemId;
    private String workItem;
    private Float workedHours;
    private Date startTime;
    private Date endTime;
    private Date recordDate;

}
