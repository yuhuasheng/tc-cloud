package com.foxconn.plm.integrate.agile.domain;

import lombok.Data;

@Data
public class PrtPartInfo {
    private String itemNum;
    private String descrEn;
    private String descrZh;
    private String rev2d;
    private String rev3d;

    private String procurementMethods;
    private String sourcingType;
    private String un;
    private String module;
    private String commodityType;
    private String material;
    private String outerPart;
    private String customer;
    private String customerPN;
    private String familyPartNumber;
    private String objectType;
}
