package com.foxconn.plm.tcservice.projectReport;

import lombok.Data;

@Data
public class QueryEntity {

    private String bu = "";
    private String customer = "";
    private String customerLike = "";
    private String productLine = "";
    private String series = "";

    private String projectName = "";
    private String dept = "";
    private String phase = "";
    private String date;
    private String month;


}
