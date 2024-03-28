package com.foxconn.plm.tcreport.drawcountreport.domain;

import lombok.Data;

/**
 * @Author HuashengYu
 * @Date 2023/1/4 13:49
 * @Version 1.0
 */
@Data
public class QueryBean {

    private String bu;
    private String customer;
    private String productLine;
    private String projectSeries;
    private String reportDate;
    private String projectName;
    private String projectId;
}
