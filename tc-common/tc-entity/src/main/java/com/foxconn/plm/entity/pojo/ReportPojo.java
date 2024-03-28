package com.foxconn.plm.entity.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReportPojo {
    private String projectId;
    private String projectName;
    private String customer;
    private String productLine;
    private String levels;
    private String phase;
    private String series;
    private String startTime;
    private String endTime;
    private String bu;
    private String ownerName;
    private List<PhasePojo> phases = new ArrayList<>();
}
