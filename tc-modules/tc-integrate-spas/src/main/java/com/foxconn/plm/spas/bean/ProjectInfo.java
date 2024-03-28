package com.foxconn.plm.spas.bean;

import lombok.Data;

import java.util.Date;

@Data
public class ProjectInfo {
    private String projectId;
    private String projectName;
    private int currentPhase;
    private int projectStatus;
    private Date projectEndTime;
    private Date phaseEndTime;
}
