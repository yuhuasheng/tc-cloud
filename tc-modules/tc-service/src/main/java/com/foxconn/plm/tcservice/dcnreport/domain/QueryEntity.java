package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

/**
 * @Author HuashengYu
 * @Date 2022/10/22 10:28
 * @Version 1.0
 */
@Data
public class QueryEntity {
    private String bu;
    private String customer;
    private String productLine;
    private String projectId;
    private String dcnRelease;
    private String dcnCostImpact;
    private String startDate;
    private String endDate;
}
