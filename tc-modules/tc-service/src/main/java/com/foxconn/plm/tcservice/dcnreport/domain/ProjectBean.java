package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

/**
 * @Author MW00333
 * @Date 2023/5/11 15:55
 * @Version 1.0
 */
@Data
public class ProjectBean {

    private String projectId;
    private String projectName;

    public ProjectBean(String projectId, String projectName) {
        this.projectId = projectId;
        this.projectName = projectName;
    }
}
