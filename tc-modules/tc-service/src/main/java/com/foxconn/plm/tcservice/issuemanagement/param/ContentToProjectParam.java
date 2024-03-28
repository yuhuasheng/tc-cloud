package com.foxconn.plm.tcservice.issuemanagement.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/16 16:47
 **/
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentToProjectParam {
    private String projectId;
    private String itemUid;
}
