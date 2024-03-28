package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

/**
 * @Author MW00333
 * @Date 2023/5/6 14:37
 * @Version 1.0
 */
@Data
public class FeeLovEntity {

    private String bu = "DT";
    private String owner;
    private String projectId;
    private String projectName;
}
