package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

/**
 * @Author HuashengYu
 * @Date 2022/10/22 15:30
 * @Version 1.0
 */
@Data
public class PlasticBean {
    private String item;
    private String dcnNo = "";
    private String hhpn = "";
    private String customerPN = "";
    private String partName = "";
    private String description = "";
    private String reason;
    private String costImpact;
    private String createDate;
}
