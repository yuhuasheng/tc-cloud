package com.foxconn.plm.tcservice.issuemanagement.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/22 14:48
 **/
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddIssueUpdatesParam {
    private String state;
    private String taskActualUser;
    private String taskOwner;
    private String response;
    private Boolean mail;
    private String itemRevUid;
    private String actualUser;
    private String userUid;
    private String groupUid;
}
