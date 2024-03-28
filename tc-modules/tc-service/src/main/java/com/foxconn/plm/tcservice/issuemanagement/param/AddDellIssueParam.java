package com.foxconn.plm.tcservice.issuemanagement.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 新增DellIssue參數類
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/19 16:27
 **/
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddDellIssueParam {
    private String actualUser;
    private String project;
    private String issueType;
    private String originVendor;
    private String originGroup;
    private String lobFound;
    private String platformFound;
    private String component;
    private String groupActivity;
    private String groupLocation;
    private String phaseFound;
    private String hardwareBuildVersion;
    private String discoveryMethod;
    private String testCaseNumber;
    private String platformIndependent;
    private String discretionaryLabels;
    private String classify;
    private String subClassify;
    private String productImpact;
    private String customerImpact;
    private String likelihood;
    private String rpn;
    private String issueSeverity;
    private String affectedOs;
    private String affectedLanguages;
    private String affectedItems;
    private String partsForProjectAffect;
    private String summary;
    private String description;
    private String stepsToReproduce;
    private String userUid;
    private String groupUid;
    private String tcProject;
}
