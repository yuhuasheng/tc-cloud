package com.foxconn.plm.tcservice.certificate;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class ProjectPojo {
    private String pmWorkId;

    private String pmName;

    private String pmMail;

    private String projectId;

    private String projectName;

    private String bu;

    @Override
    public boolean equals(Object object) {
        if (object instanceof ProjectPojo) {
            ProjectPojo otherProject = (ProjectPojo) object;
            if (StringUtils.hasLength(projectId) && StringUtils.hasLength(otherProject.getProjectId())) {
                return projectId.equalsIgnoreCase(otherProject.getProjectId());
            }
        }
        return super.equals(object);
    }
}
