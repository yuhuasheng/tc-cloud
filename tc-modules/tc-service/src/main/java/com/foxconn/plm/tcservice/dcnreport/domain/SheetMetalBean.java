package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

/**
 * @Author HuashengYu
 * @Date 2022/10/22 15:28
 * @Version 1.0
 */
@Data
public class SheetMetalBean {

    private String item;
    private String dcnNo = "";
    private String modelNo = "";
    private String projectName = "";
    private String hhpn = "";
    private String customerPN = "";
    private String partName = "";
    private String description = "";
    private String reason;
    private String costImpact;
    private String createDate;
}
