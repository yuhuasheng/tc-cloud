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
public class AddHpIssueParam {
    private String actualUser;
    private String state;
    private String status;
    private String priority;
    private String division;
    private String originatorWorkgroup;
    private String primaryProduct;
    private String productVersion;
    private String productLine;
    private String componentType;
    private String componentSubSystem;
    private String component;
    private String componentVersion;
    private String componentLocalization;
    private String componentPartNumber;
    private String frequency;
    private String gatingMilestone;
    private String testEscape;
    private String severity;
    private String impacts;
    private String shortDesc;
    private String longDesc;
    private String stepsToReproduce;
    private String customerImpact;
    private String userUid;
    private String groupUid;
    private String tcProject;
}
