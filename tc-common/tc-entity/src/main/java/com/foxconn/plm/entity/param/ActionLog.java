package com.foxconn.plm.entity.param;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel
public class ActionLog {

    private Long id;

    private String creator;
    private String userName;
    private String project;

    private String phase;
    private String bu;
    private String customer;
    private String dept;
    private String projLevel;
    private String startTime;

    private String endTime;
    private String handleResult;
    private String msg;
    private String itemId;

    private String phaseEndDate;

}
