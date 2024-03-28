package com.foxconn.plm.tcservice.issuemanagement.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/20 9:55
 **/
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddLenovoIssueParam {
    private String actualUser;
    private String name;
    private String requestPriority;
    private String release;
    private String productionLine;
    private String component;
    private String description;
    private String releaseOther;
    private String operationSys;
    private String operationSysOther;
    private String phaseFound;
    private String reproduceSteps;
    private String defectConsistency;
    private String affectedSystem;
    private String limitation;
    private String brand;
    private String closeDate;
    private String answerCode;
    private String remark;
    private String configuration;
    private String userUid;
    private String groupUid;
    private String tcProject;
}
