package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

/**
 * @Author HuashengYu
 * @Date 2022/10/21 11:06
 * @Version 1.0
 */
@Data
public class DCNReportBean implements Comparable<DCNReportBean> {
    private String item;
    private String bu;
    private String dcnNo = "";
    private String modelNo = "";
    private String projectId = "";
    private String projectName = "";
    private String hhpn = "";
    private String customerPN = "";
    private String partName = "";
    private String description = "";
    private String reason;
    private String costImpact;
    private String status;
    private String customerType;
    private String productLine;
    private String createDate;
    private String modelNoPrefix;
    private String owner;

    @Override
    public int compareTo(DCNReportBean o) {
        int i = this.dcnNo.compareTo(o.getDcnNo());
        return i;
    }
}
